```java
package com.sismics.reader.rest;

import com.sun.jersey.multipart.FormDataBodyPart;
import com.sun.jersey.multipart.FormDataMultiPart;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;
import org.junit.Before;
import org.junit.Test;

import javax.ws.rs.core.MediaType;
import java.io.BufferedInputStream;
import java.io.InputStream;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;

public class TestJobResource extends BaseJerseyTest {

    private String job1UserId;
    private String job2UserId;

    @Before
    public void setUp() throws Exception {
        job1UserId = createUser("job1");
        job2UserId = createUser("job2");
    }

    @Test
    public void testJobResource() throws Exception {
        importOpml(job1UserId);
        checkUserJobs(job1UserId);
        checkForbiddenDeleteJob(job2UserId, job1UserId);
        deleteJob(job1UserId);
        checkNoJobs(job1UserId);
    }

    private void checkForbiddenDeleteJob(String userId, String jobOwnerId) throws Exception {
        login(userId);
        DELETE("/job/" + jobOwnerId);
        assertIsBadRequest();
    }

    private void importOpml(String userId) throws Exception {
        login(userId);
        FormDataMultiPart form = new FormDataMultiPart();
        InputStream track = this.getClass().getResourceAsStream("/import/greader_subscriptions.xml");
        FormDataBodyPart fdp = new FormDataBodyPart("file",
                new BufferedInputStream(track),
                MediaType.APPLICATION_OCTET_STREAM_TYPE);
        form.bodyPart(fdp);
        PUT("/subscription/import", form);
        assertIsOk();
    }

    private void checkUserJobs(String userId) throws Exception {
        GET("/user");
        assertIsOk();
        JSONObject json = getJsonResult();
        JSONArray jobs = json.getJSONArray("jobs");
        assertEquals(1, jobs.length());
        JSONObject job = (JSONObject) jobs.get(0);
        String jobId = job.getString("id");
        assertNotNull(jobId);
        assertEquals("import", job.optString("name"));
        assertNotNull(job.optString("start_date"));
        assertNotNull(job.optString("end_date"));
        assertEquals(4, job.optInt("feed_success"));
        assertEquals(0, job.optInt("feed_failure"));
        assertEquals(4, job.optInt("feed_total"));
        assertEquals(0, job.optInt("starred_success"));
        assertEquals(0, job.optInt("starred_failure"));
        assertEquals(0, job.optInt("starred_total"));
    }

    private void deleteJob(String userId) throws Exception {
        login(userId);
        JSONObject json = getJsonResult();
        JSONArray jobs = json.getJSONArray("jobs");
        String jobId = ((JSONObject) jobs.get(0)).getString("id");
        DELETE("/job/" + jobId);
        assertIsOk();
        json = getJsonResult();
        assertEquals("ok", json.getString("status"));
    }

    private void checkNoJobs(String userId) throws Exception {
        GET("/user");
        assertIsOk();
        JSONObject json = getJsonResult();
        JSONArray jobs = json.getJSONArray("jobs");
        assertEquals(0, jobs.length());
    }
}
```