const axios = require('axios')
const https = require('https');
const fs = require('fs/promises')
const createReadStream = require('fs').createReadStream;
const FormData = require('form-data');
const config = require('./config.json')

// Testing
const apiToken = config.isTesting?config.kiraApiTokenTest:config.kiraApiTokenProd
const endpoint = config.kiraEndpoint


const getProjects = async () => {
    let ret = null
    try {
        const response = await axios.get(endpoint + "projects", { headers: { Authorization: apiToken, accept: "application/json" } });
        ret = response.data
    }
    catch (error) {
        console.log(error)
    }
    return ret;
}

const getDocuments = async () => {
    let ret = null;
    try {
        const response = await axios.get(endpoint + "documents/?fields=document_id&fields=document_name&fields=folder_path&fields=upload_time&fields=document_types&fields=is_folder&fields=reviewer_ids&fields=assignee_ids", { headers: { Authorization: apiToken, accept: "application/json" } });
        ret = response.data
        // await fs.writeFile("documents.json", JSON.stringify(response.data), 'utf8')
    }
    catch (error) {
        console.log(error)
    }
    return ret;
}

const getDocumentName = async (did) => {
    let ret = null;
    try {
        const response = await axios.get(endpoint + `documents/${did}?fields=document_id&fields=document_name`, { headers: { Authorization: apiToken, accept: "application/json" } });
        ret = response.data
        // await fs.writeFile("documents.json", JSON.stringify(response.data), 'utf8')
    }
    catch (error) {
        console.log(error)
    }
    return ret;
}


const getDocumentFields = async (did) => {
    return await getDocumentProperties(did, "fields?include-instances=false&include-summaries=false")
}

const getDocumentFieldAnswers = async (did) => {
    return await getDocumentProperties(did, "field-answers")
}

const getDocumentFieldAnswersAndSummary = async (did) => {
    let answers = await getDocumentFieldAnswers(did)
    if (answers === null) return null;
    let summary = await getDocumentFieldSummary(did)
    const m = {};
    for (let i = 0; i < summary.length; i++) {
        m[summary[i].field_id] = { field_summary_id: summary[i].field_summary_id, text: summary[i].text }
    }
    for (let i = 0; i < answers.length; i++) {
        if (m[answers[i].field_id]) {
            answers[i]["field_summary_id"] = m[answers[i].field_id].field_summary_id
            answers[i]["text"] = m[answers[i].field_id].text
        }
    }
    return answers;
}


const getDocumentTags = async (tag_text = null) => {
    let ret = null;
    try {
        const response = await axios.get(endpoint + `tags?start=1&count=10000&type=document&${tag_text ? 'tag_text=' + tag_text : ''}`, { headers: { Authorization: apiToken, accept: "application/json" } });
        ret = response.data;
    }
    catch (error) {
        console.log(error)
    }
    return ret;
}

const getDocumentFieldSummary = async (did) => {
    let data = await getDocumentProperties(did, "field-summaries")
    return data
}

const getDocumentFieldInstances = async (did) => {
    return await getDocumentProperties(did, "field-instances?fields=field_id&fields=field_name&fields=field_instance_id&fields=text&fields=page_range&fields=text_ranges")
}

const getDocumentProperties = async (did, p) => {
    let ret = null;
    try {
        const response = await axios.get(endpoint + `documents/${did}/${p}`, { headers: { Authorization: apiToken, accept: "application/json" } });
        ret = response.data
    }
    catch (error) {
        console.log(error)
    }
    return ret;
}

const getCompletedJobs = async () => {
    let ret = null;
    try {
        const response = await axios.get(endpoint + `jobs/?job_status=completed`, { headers: { Authorization: apiToken, accept: "application/json" } });
        ret = response.data
    }
    catch (error) {
        console.log(error)
    }
    return ret;

}

const getJobStatus = async (jid) => {
    let ret = null;
    try {
        const response = await axios.get(endpoint + `jobs/${jid}/status`, { headers: { Authorization: apiToken, accept: "application/json" } });
        ret = { status: response.status }
        // in progress
        switch (response.status) {
            case 200:
                // get next try time
                ret = { ...ret, desc: "processing", ...response.data };
                break;
            case 303:   // completed
                ret = { ...ret, desc: "completed", ...response.data };
                break;
        }
    }
    catch (error) {
        switch (error.response.status) {
            case 303:
                ret = { status: 303, desc: "completed" };
                break;
            case 404:   // Not found
                ret = { status: 404, desc: "not found" };
                break;
            case 410:   // Gone
                ret = { status: 410, desc: "The job is no longer available" };
                break;
        }
    }
    return ret;
}
// tagText can be any text with limited length
const setDocTag = async (did, tagText) => {
    let ret = null;
    try {
        const response = await axios.post(endpoint + `tags?document_id=${did}`, { "tag_text": tagText }, { headers: { Authorization: apiToken, accept: "application/json" } });
        if (response.status === 201)
            ret = response.data
        console.log(response.data)
    }
    catch (error) {
        console.log(error)
    }
    return ret;

}

const delDocTag = async (tagID) => {
    let ret = null;
    try {
        const response = await axios.delete(endpoint + `tags/${tagID}`, { headers: { Authorization: apiToken, accept: "application/json" } });
        if (response.status === 204) // 403 insufficient permission 404 not exist
        {
            ret = { status: "succeed" };
        }
    }
    catch (error) {
        console.log(error)
    }
    return ret;
}

const uploadFile = async (projectID, fileName, folder = null) => {
    let ret = null;
    try {
        const formData = new FormData();
        formData.append("file", createReadStream(fileName));
        formData.append("project_id", projectID);        //,folder_path:'/Demo/'
        if (folder !== null)
            formData.append("folder_path", folder);

        const response = await axios({
            method: 'post',
            url: endpoint + `documents`,
            data: formData,
            maxContentLength: Infinity,
            maxBodyLength: Infinity,
            headers: {
                Authorization: apiToken,
                "Content-Type": "multipart/form-data",
                accept: "application/json",
                ...formData.getHeaders()
            }
        })

        switch (response.status) {
            case 200:
            case 201:
                // console.log(response.data)
                ret = response.data
            case 500:
            case 501:
                break;
        }
        // console.log(response)
    }
    catch (error) {
        console.log(error)
        console.log('---------------')
        console.log(JSON.stringify(error.response.data))
    }
    return ret;
}

module.exports = {
    getProjects,
    getDocuments,
    getDocumentName,
    getDocumentFieldAnswersAndSummary,
    getDocumentFieldInstances,
    getDocumentTags,
    uploadFile,
    setDocTag,
    delDocTag,
    getJobStatus,
    getCompletedJobs
}

