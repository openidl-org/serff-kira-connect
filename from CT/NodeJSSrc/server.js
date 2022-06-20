const util = require('util');
const exec = util.promisify(require('child_process').exec);
const os = require('os')
const config = require('./config.json')
const kira = require('./kirautility')
const path = require('path');
const nodemailer = require('nodemailer');

const seperator = path.sep
const eol = os.EOL;
const fs = require('fs');
const fsPromise = fs.promises

const filefolder = __dirname + path.sep + "data"
const projectMap = config.projectMap;
const initTag = "SERFFImport"
const kiraFolder = "api"
let isTerminating = false;
const sleepTime = config.sleepTime;
require('winston-daily-rotate-file');
const { createLogger, transports, format } = require('winston');
const { printf } = format;

const transport = nodemailer.createTransport({
    name: config.mailHost,
    host: config.mailHost,
    port: 25,
    // pool: true,
    // maxConnections: 1,
    // maxMessages: 1,
    tls: { rejectUnauthorized: false },
    // auth: {
    //    user: 'put_your_username_here',
    //    pass: 'put_your_password_here'
    // }
});



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

// text instances are not in the fields with answers
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
        else {   // text field with instance
            m[v.field_id] = { instances: [v], field_id: v.field_id, field_name: v.field_name }
        }
    });

    let v = Object.values(m)
    let content = ""
    let contentObj = {};
    let yes = 0;
    let no = 0;

    const getID = (name)=>{
        let p = name.indexOf(' ')
        return name.substring(0,p)
    }
    for (let i = 0; i < v.length; i++) {
        let type = "text"
        let id = getID(v[i].field_name);
        if (v[i].answer) {
            // answered no and not in the expected list
            if (v[i].answer === "no"&&!config.expectedNo[id]) {
                content += `${v[i].field_name}:  ${v[i].answer}\r\n`;
                contentObj[id] = `${v[i].field_name}:  ${v[i].answer}\r\n`;
                type = "no"
                no++;
            }
            else if (v[i].answer === "yes") {
                type = "yes"
                yes++;
            }
        }

        let instances = v[i].instances
        if (instances) {
            let id = getID(v[i].field_name);
            if (type === "text")
            {
                contentObj[id] = `${v[i].field_name}: \r\n`;
                content += `${v[i].field_name}: \r\n`;
            }
                
            if (type === "no" || type === "text") {
                for (let j = 0; j < instances.length; j++) {
                    contentObj[id] += `    ${instances[j].text}      Page Range ${JSON.stringify(instances[j].page_range)}     Text Ranges ${JSON.stringify(instances[j].text_ranges)}\r\n`
                    content += `    ${instances[j].text}      Page Range ${JSON.stringify(instances[j].page_range)}     Text Ranges ${JSON.stringify(instances[j].text_ranges)}\r\n`
                }
            }
        }
        content += `\r\n`
    }
    // sort by id
    let arr = Object.entries(contentObj)
    arr.sort();
    content = ""
    for (let i=0;i<arr.length;i++){
        content += arr[i][1];
        content += `\r\n`
    }
    console.log(content)
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
            else {
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
                    let p1 = files[i].lastIndexOf('-')
                    let p2 = files[i].lastIndexOf('.');
                    let toi = files[i].substring(p1+1,p2);
                    let projectid = projectMap[toi];
                    if (!projectid){
                        projectid = projectMap['default']
                    }
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
            let emailContent = ""
            for (let i = 0; i < documents.length; i++) {
                let docid = documents[i].document_id
                let tagid = documents[i].tag_id
                let name = await kira.getDocumentName(docid);
                name = name.document_name

                if (name.startsWith("Serff")) {
                    let pt  = name.lastIndexOf('-')
                    let serff = name.substring(6, pt)
                    // remove sequence number if it's a duplicate
                    // let p = serff.indexOf(" ");
                    // if (p > -1)
                    //     serff = serff.substring(0, p)
                    let fields = await kira.getDocumentFieldAnswersAndSummary(docid)
                    if (fields.length > 0) {
                        let instances = await kira.getDocumentFieldInstances(docid)
                        let details = processFieldsInstances(fields, instances);
                        // console.log(details)
                        emailContent += "<p>";
                        if (details.no > 0) {
                            result.push({ docid, serff, details: details.content })
                            emailContent += ("Creating Objection Letter:" + serff);
                        }
                        else {
                            result.push({ docid, serff, details: "" })
                            emailContent += ("Dismissing " + serff);
                        }
                        emailContent += "</p><p>";
                        emailContent += details.content.replaceAll('\r\n\r\n', '</p><p>')
                        emailContent = emailContent.substring(0, emailContent.length - 3); // remove last <p>
                        // emailContent+=JSON.stringify(details);
                        emailContent += "<p>--------------------------------------------</p>";
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
                if (emailContent.length > 0) {
                    sendNotifyEmail(emailContent)
                }
            }
            else {
                //console.log("No Kira Document to Process")
            }
        }
        if (result.length > 0) {
            console.log("3. Fetched KIRA result", result.length)
            eventLogger.info("3. Fetched KIRA result: " + result.length)
        }
        return result.length;
    }
    catch (error) {
        console.log(error)
        eventLogger.error("Error " + error)
    }
    return 0;
}

const isWorkingHour = () => {
    let hour = new Date().getHours();
    return hour > 5 && hour < 19
}

const sendNotifyEmail = async (content) => {
    for (let i = 0; i < config.emails.length; i++) {
        sendEmail("cid-donot-reply@ct.gov", config.emails[i], "KIRA SERFF Report", content);
    }
}

const sendEmail = async (from, to, subject, html) => {
    try {
        const message = { from, to, subject, html };
        console.log(subject, html);
        if (!config.testEmail) {
            let info = await transport.sendMail(message);
            eventLogger.info(`Email sent from ${from} TO ${to};Subject ${subject};Content:${html};Info:${info.response}`);
            console.log(`Email Sent: Subject ${subject};Info:${info.response}`)
        }
        return true;
    }
    catch (error) {
        console.log(error)
        eventLogger.error(error + ",sendEmail");
    }

    eventLogger.error(`Failed to send email sent from ${from}-to ${to};Subject ${subject};Content ${html}`);

    return false;
}



const getSleepTime = () => {
    // if (config.isTesting)
    //     return sleepTime
    return isWorkingHour() ? sleepTime : 1800000; // if not working hour, run every 30 minutes
}
const runIt = async () => {
    try {
        if (isTerminating) {
            eventLogger.info("Server Stopped by User")
            process.exit(0);
        }
        else {
            console.log(new Date().toLocaleString())
            timeoutHandler = null;
            await runJava();
            await uploadToKIRA();
            const waitTime = getSleepTime();
            let n = await getKiraResult();
            // with result, don't wait
            if (n > 0) {
                runIt();
                return;
            }
            else {
                eventLogger.info(`Sleep ${waitTime / 1000} seconds...`)
                console.log(`Sleep ${waitTime / 1000} seconds...`)
                console.log("-----------------------------")
                if (isTerminating)
                    process.exit(0);
                setTimeout(async () => {
                    if (isTerminating)
                        process.exit(0);
                    else
                        await runIt();

                }, waitTime)
            }
        }
    }
    catch (error) {
        console.log("Something is wrong", error)
        eventLogger.error("Error " + error)
        eventLogger.info("Server Stopped")
        process.exit(1);
    }
    finally {
    }
}
process.on('beforeExit', (code) => {
    // console.log('Process beforeExit event with code: ', code);
});

process.on('SIGTERM', () => {
    if (isTerminating) {
        clearTimeout(timeoutHandler)
        eventLogger.info("Server Stopped")
        process.exit(0)
    }

    isTerminating = true;
});
process.on('SIGINT', () => {
    if (isTerminating) {
        clearTimeout(timeoutHandler)
        console.log("Terminating...")
        eventLogger.info("Server Stopped")
        process.exit(0)
    }
    isTerminating = true;
    console.log("Interrupting, will exit in the next cycle")
});

const test = async () => {
    let fields = await kira.getDocumentFieldAnswersAndSummary(312)
    if (fields.length > 0) {
        let instances = await kira.getDocumentFieldInstances(312)
        let details = processFieldsInstances(fields, instances);
        console.log(details)
    }
}

// test();
eventLogger.info("Server Started")
runIt();
// uploadToKIRA();
// getKiraResult();






// NWCM-129244780, duplicate returned different result