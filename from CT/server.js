const util = require('util');
const exec = util.promisify(require('child_process').exec);
const os = require('os')
const config = require('./config.json')
const kira = require('./kirautility')
const path = require('path');
const seperator = path.sep
const eol = os.EOL;
const fs = require('fs');
const fsPromise = fs.promises

const filefolder = __dirname + path.sep + "data"
const projectid = config.projectID;
const initTag = "SERFFImport"
const kiraFolder = "api"
let isTerminating = false;
const sleepTime = config.sleepTime;
require('winston-daily-rotate-file');
const { createLogger, transports, format } = require('winston');
const { printf } = format;

const myFormat = printf(({ level, message, timestamp }) => {
    return getCurrentTimeStamp() + ` ${level}: ${message}`;
});
const getCurrentTimeStamp = () => {
    return new Date().toLocaleString('en-US', {
        timeZone: 'America/New_York'
    });
}

console.log(filefolder)
const eventtransport = new (transports.DailyRotateFile)({
    filename: './log/Event-%DATE%.log',
    datePattern: 'YYYY-MM-DD',
    zippedArchive: false,
    maxSize: '20971520',
    maxFiles: '30d', // keep 3 days record for testing
    format: myFormat,
    level: 'info'
});

eventtransport.on('rotate', function (oldFilename, newFilename) {
    // do something fun
});

const eventLogger = createLogger({
    transports: [
        eventtransport
    ]
});

// exports.logger = eventLogger;





const processFieldsInstances = (fields, instances) => {
    let m = {}
    fields.forEach(v => {
        m[v.field_id] = v;
    });
    instances.forEach(v => {
        if (m[v.field_id]) {
            if (m[v.field_id]["instances"]) m[v.field_id]["instances"].push(v);
            else
                m[v.field_id]["instances"] = [v];
        }
    });

    let v = Object.values(m)
    let content = ""
    let yes = 0;
    let no = 0;
    for (let i = 0; i < v.length; i++) {
        if (v[i].answer === "yes") yes++;
        else no++;
        content += `${v[i].field_name}:  ${v[i].answer}\r\n`
        let instances = v[i].instances
        if (instances) {
            for (let j = 0; j < instances.length; j++) {
                content += `    ${instances[j].text}      Page Range ${JSON.stringify(instances[j].page_range)}     Text Ranges ${JSON.stringify(instances[j].text_ranges)}\r\n`
            }
        }
        content += `\r\n`
    }
    // console.log('----------------------------------')
    return { yes, no, content }
}
// Run external java process download serff files and consume kira extraction json file
const runJava = async () => {
    if (isTerminating)
        return;
    try {
        const { stdout, stderr } = await exec(`java -cp ${__dirname}${path.sep}javalib${path.sep}serff.jar serffproc.serffjob`);
        if (stderr) {
            if (!isTerminating) {
                console.log(stderr, "Failed");
                eventLogger.error("Failed: " + stderr)
            }
            else{
                eventLogger.error("Process Terminated: " + stderr)
            }
        }
        else {
            let result = stdout.trim().split(eol);
            if (result[result.length - 1] === "Downloaded 0 Processed 0")
                return;
            console.log("1. SERFF Processed")
            eventLogger.info("1. SERFF Processed")
            for (let i = 0; i < result.length; i++) {
                console.log(result[i])
                eventLogger.info(result[i])

            }
        }
    }
    catch (err) {
        console.log("Error", err);
        eventLogger.error("Error: " + err)
    }
    finally {
    }
}


const uploadToKIRA = async () => {
    // upload files in the filefolder to KIRA, if succeed, delete files
    try {
        let files = await fsPromise.readdir(filefolder);
        if (files.length > 0) {
            console.log("2. Adding files to KIRA:", files.length)
            eventLogger.info("2. Adding files to KIRA:", files.length)
            for (let i = 0; i < files.length; i++) {
                if (files[i].startsWith("Serff")) {
                    let file = filefolder + seperator + files[i]
                    let data = await kira.uploadFile(projectid, file, kiraFolder)
                    if (data !== null) {
                        console.log("Uploaded", file, data)
                        eventLogger.info("Uploaded " + file + JSON.stringify(data))
                        let tagData = await kira.setDocTag(data.document_id, initTag)
                        eventLogger.info("Added tag " + JSON.stringify(tagData))
                        console.log("Added tag", tagData)
                        // delete the file
                        if (tagData !== null && data !== null) {
                            await fsPromise.unlink(file)
                        }
                    }
                }
            }
        }
    }
    catch (error) {
        console.log(error)
        eventLogger.error("Error " + error)
    }
}

const getKiraResult = async () => {
    try {
        let documents = await kira.getDocumentTags(initTag)
        let result = [];
        if (documents) {
            // Collect detail information and save in the staging json file for Java to consume
            for (let i = 0; i < documents.length; i++) {
                let docid = documents[i].document_id
                let tagid = documents[i].tag_id
                let name = await kira.getDocumentName(docid);
                name = name.document_name
                if (name.startsWith("Serff")) {
                    let serff = name.substring(6, name.length - 4)
                    // remove sequence number if it's a duplicate
                    let p = serff.indexOf(" ");
                    if (p>-1)
                        serff = serff.substring(0, p)
                    let fields = await kira.getDocumentFieldAnswersAndSummary(docid)
                    if (fields.length > 0) {
                        let instances = await kira.getDocumentFieldInstances(docid)
                        let details = processFieldsInstances(fields, instances);
                        // console.log(details)
                        if (details.no > 0) {
                            result.push({ docid, serff, details: details.content })
                        }
                        else
                            result.push({ docid, serff, details: "" })
                        // remove tag initTag
                        // console.log("delete tag " + tagid);
                        kira.delDocTag(tagid);
                        console.log("Collected data of", serff);
                        eventLogger.info("Collected data of " + serff)
                    }
                }
            }
            if (result.length > 0) {
                await fsPromise.writeFile(filefolder + seperator + "kira.json", JSON.stringify(result))
                console.log("Saved KIRA data to kira.json")
                eventLogger.info("Saved KIRA data to kira.json")
            }
            else {
                //console.log("No Kira Document to Process")
            }
        }
        if (result.length > 0) {
            console.log("3. Fetched KIRA result", result.length)
            eventLogger.info("3. Fetched KIRA result: " + result.length)
        }
    }
    catch (error) {
        console.log(error)
        eventLogger.error("Error " + error)
    }

}
const runIt = async () => {
    try {
        if (isTerminating)
            process.exit(0);
        else {
            console.log(new Date().toLocaleString())
            timeoutHandler = null;
            await runJava();
            await uploadToKIRA();
            await getKiraResult();
            // eventLogger.info(`Sleep ${sleepTime / 1000} seconds...`)
            console.log(`Sleep ${sleepTime / 1000} seconds...`)
            console.log("-----------------------------")
            if (isTerminating)
                process.exit(0);
                setTimeout(async () => {
                if (isTerminating)
                    process.exit(0);
                else
                    await runIt();

            }, sleepTime)
        }
    }
    catch (error) {
        console.log("Something is wrong",error)
        eventLogger.error("Error " + error)
        process.exit(1);
    }
    finally{
    }
}
process.on('beforeExit', (code) => {
    // console.log('Process beforeExit event with code: ', code);
});

process.on('SIGTERM', () => {
    if (isTerminating){
        clearTimeout(timeoutHandler)
        process.exit(0)
    }
        
    isTerminating = true;
});
process.on('SIGINT', () => {
    if (isTerminating){
        clearTimeout(timeoutHandler)
        console.log("Terminating...")
        process.exit(0)
    }
    isTerminating = true;
    console.log("Interrupting, will exit in the next cycle")
});

runIt();


// console.log(processFieldsInstances(sample))





// Workflow:
/*
    1. Run external java process download serff files and consume kira extraction json file
    2. Upload serff files to kira, construct json files for java processing to create objection letters or dismiss case

*/
