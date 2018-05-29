[1mdiff --git a/spring-cloud-skipper-server-core/src/test/java/org/springframework/cloud/skipper/server/AbstractAssertReleaseDeployedTest.java b/spring-cloud-skipper-server-core/src/test/java/org/springframework/cloud/skipper/server/AbstractAssertReleaseDeployedTest.java[m
[1mindex 9146607..1c377f3 100644[m
[1m--- a/spring-cloud-skipper-server-core/src/test/java/org/springframework/cloud/skipper/server/AbstractAssertReleaseDeployedTest.java[m
[1m+++ b/spring-cloud-skipper-server-core/src/test/java/org/springframework/cloud/skipper/server/AbstractAssertReleaseDeployedTest.java[m
[36m@@ -41,8 +41,8 @@[m [mpublic abstract class AbstractAssertReleaseDeployedTest {[m
 		CountDownLatch latch = new CountDownLatch(1);[m
 		long startTime = System.currentTimeMillis();[m
 		while (!isDeployed(releaseName, releaseVersion)) {[m
[31m-			if ((System.currentTimeMillis() - startTime) > 120000) {[m
[31m-				logger.info("Stopping polling for deployed status after 60 seconds for release={} version={}",[m
[32m+[m			[32mif ((System.currentTimeMillis() - startTime) > 180000) {[m
[32m+[m				[32mlogger.info("Stopping polling for deployed status after 3 minutesand  for release={} version={}",[m
 						releaseName, releaseVersion);[m
 				fail("Could not determine if release " + releaseName + "-v" + releaseVersion +[m
 						" was deployed successfully, timed out polling.");[m
[1mdiff --git a/spring-cloud-skipper-server-core/src/test/java/org/springframework/cloud/skipper/server/controller/docs/InstallDocumentation.java b/spring-cloud-skipper-server-core/src/test/java/org/springframework/cloud/skipper/server/controller/docs/InstallDocumentation.java[m
[1mindex 2ef2eeb..c57f75e 100644[m
[1m--- a/spring-cloud-skipper-server-core/src/test/java/org/springframework/cloud/skipper/server/controller/docs/InstallDocumentation.java[m
[1m+++ b/spring-cloud-skipper-server-core/src/test/java/org/springframework/cloud/skipper/server/controller/docs/InstallDocumentation.java[m
[36m@@ -27,6 +27,7 @@[m [mimport org.springframework.cloud.skipper.domain.Release;[m
 import org.springframework.cloud.skipper.domain.StatusCode;[m
 import org.springframework.http.MediaType;[m
 import org.springframework.test.context.ActiveProfiles;[m
[32m+[m[32mimport org.springframework.test.web.servlet.MvcResult;[m
 import org.springframework.util.StringUtils;[m
 [m
 import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.post;[m
[36m@@ -57,7 +58,7 @@[m [mpublic class InstallDocumentation extends BaseDocumentation {[m
 		final MediaType contentType = new MediaType(MediaType.APPLICATION_JSON.getType(),[m
 				MediaType.APPLICATION_JSON.getSubtype(), Charset.forName("utf8"));[m
 [m
[31m-		mockMvc.perform(post("/api/package/install").accept(MediaType.APPLICATION_JSON).contentType(contentType)[m
[32m+[m		[32mMvcResult result = mockMvc.perform(post("/api/package/install").accept(MediaType.APPLICATION_JSON).contentType(contentType)[m
 				.content(convertObjectToJson(installRequest))).andDo(print())[m
 				.andExpect(status().isCreated())[m
 				.andDo(this.documentationHandler.document([m
[36m@@ -113,7 +114,10 @@[m [mpublic class InstallDocumentation extends BaseDocumentation {[m
 								fieldWithPath("configValues.raw")[m
 										.description("The raw YAML string of configuration values"),[m
 								fieldWithPath("manifest.data").description("The manifest of the release"),[m
[31m-								fieldWithPath("platformName").description("Platform name of the release"))));[m
[32m+[m								[32mfieldWithPath("platformName").description("Platform name of the release"))))[m
[32m+[m				[32m.andReturn();[m
[32m+[m		[32mRelease release = convertContentToRelease(result.getResponse().getContentAsString());[m
[32m+[m		[32massertReleaseIsDeployedSuccessfully(releaseName, release.getVersion());[m
 	}[m
 [m
 	@Test[m
[36m@@ -136,7 +140,7 @@[m [mpublic class InstallDocumentation extends BaseDocumentation {[m
 		final MediaType contentType = new MediaType(MediaType.APPLICATION_JSON.getType(),[m
 				MediaType.APPLICATION_JSON.getSubtype(), Charset.forName("utf8"));[m
 [m
[31m-		mockMvc.perform(post("/api/package/install/{packageMetaDataId}", release.getId()).accept(MediaType.APPLICATION_JSON)[m
[32m+[m		[32mMvcResult result = mockMvc.perform(post("/api/package/install/{packageMetaDataId}", release.getId()).accept(MediaType.APPLICATION_JSON)[m
 				.contentType(contentType)[m
 				.content(convertObjectToJson(installProperties2))).andDo(print())[m
 				.andExpect(status().isCreated())[m
[36m@@ -193,6 +197,9 @@[m [mpublic class InstallDocumentation extends BaseDocumentation {[m
 								fieldWithPath("configValues.raw")[m
 										.description("The raw YAML string of configuration values"),[m
 								fieldWithPath("manifest.data").description("The manifest of the release"),[m
[31m-								fieldWithPath("platformName").description("Platform name of the release"))));[m
[32m+[m								[32mfieldWithPath("platformName").description("Platform name of the release"))))[m
[32m+[m				[32m.andReturn();[m
[32m+[m		[32mRelease release2 = convertContentToRelease(result.getResponse().getContentAsString());[m
[32m+[m		[32massertReleaseIsDeployedSuccessfully(releaseName2, release2.getVersion());[m
 	}[m
 }[m
