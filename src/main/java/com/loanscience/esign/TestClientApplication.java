package com.loanscience.esign;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.loanscience.esign.client.model.*;
import com.loanscience.esign.util.Json;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.http.*;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

@SpringBootApplication
@ConfigurationProperties()
public class TestClientApplication implements CommandLineRunner
{
	public static void main(String[] args) {
		SpringApplication.run(TestClientApplication.class, args);
	}

	@Value("${esignapi.baseURL}")
	private String baseURL = "";
	@Value("${esignapi.client.apikey}")
	private String apikey = "";
	@Value("${esignapi.client.name}")
	private String name = "";
	@Value("${esignapi.client.email}")
	private String email = "";
	@Value("${esignapi.client.templateID}")
	private String templateID = "";

	@Override
	public void run(String... args) throws Exception
	{
		createEnvelopeFromPDFWithAnchors();

		createEnvelopeFromPDFWithCoordinates();

		createEnvelopeFromTemplate();

		getRecipientURL("c45890fa-082a-4c57-a54e-7dfb5174c98d");

		downloadDocument("c45890fa-082a-4c57-a54e-7dfb5174c98d", Paths.get("/Users/mfoulk/Desktop/signed_doc.pdf"));

		System.exit(0);
	}

	/**
	 * Demostrate creating an envelope using anchors embedded in the document
	 * @throws Exception
	 */
	public void createEnvelopeFromPDFWithAnchors() throws Exception
	{
		System.out.println("Creating an Envelope with Anchors");
		byte[] doc = loadFile("withanchors.pdf");
		Map<String, List<Tab>> tabMap = new HashMap<>();
		List<Tab> tabs = new ArrayList<>();
		Tab tab = new Tab();
		tab.setName("Name");
		tab.setAnchorIgnoreIfNotPresent(false);
		tab.setAnchorString("{name}");
		tab.setWidth(150);
		tab.setRequired(true);
		tab.setMaxLength(100);
		tab.setTabLabel("Your Name");
		tabs.add(tab);

		tab = new Tab();
		tab.setName("Address");
		tab.setAnchorIgnoreIfNotPresent(false);
		tab.setAnchorString("{add}");
		tab.setWidth(300);
		tab.setRequired(true);
		tab.setMaxLength(150);
		tab.setTabLabel("Your address");
		tabs.add(tab);

		tab = new Tab();
		tab.setName("City");
		tab.setAnchorIgnoreIfNotPresent(false);
		tab.setAnchorString("{city}");
		tab.setWidth(200);
		tab.setRequired(true);
		tab.setMaxLength(150);
		tab.setTabLabel("Your city");
		tabs.add(tab);

		tab = new Tab();
		tab.setName("State Abbreviation");
		tab.setAnchorIgnoreIfNotPresent(false);
		tab.setAnchorString("{state}");
		tab.setWidth(25);
		tab.setRequired(true);
		tab.setMaxLength(2);
		tab.setTabLabel("Your state");
		tabs.add(tab);

		tab = new Tab();
		tab.setName("Zip");
		tab.setAnchorIgnoreIfNotPresent(false);
		tab.setAnchorString("{zip}");
		tab.setWidth(50);
		tab.setRequired(true);
		tab.setValidationPattern("^[0-9]{5}$");
		tab.setValidationMessage("Must be five numbers");
		tab.setMaxLength(5);
		tab.setTabLabel("Your postal code");
		tabs.add(tab);

		tabMap.put("textTabs", tabs);

		tabs = new ArrayList<>();
		tab = new Tab();
		tab.setName("Sign Here");
		tab.setAnchorIgnoreIfNotPresent(false);
		tab.setAnchorString("{s}");
		tab.setRequired(true);
		tabs.add(tab);
		tabMap.put("signHereTabs", tabs);

		tabs = new ArrayList<>();
		tab = new Tab();
		tab.setName("Date Signed");
		tab.setAnchorIgnoreIfNotPresent(false);
		tab.setAnchorString("{ds}");
		tab.setRequired(true);
		tabs.add(tab);
		tabMap.put("dateSignedTabs", tabs);

		EnvelopeRequest request = new EnvelopeRequest()
				.setEmailRequest(new EmailRequest()
						.setName(name)
						.setRecipientEmail(email)
						.setEmailSubject("Test Document using Anchors")
						.setEmailBody("This is a test document")
						.setTest(true)
				)
				.setDocument(doc)
				.setFileName("Loan_Application_" + name.replace(" ", ""))
				.setTabs(tabMap)
		;

		print(new ObjectMapper().valueToTree(request));
		post("/envelope", new HttpEntity<Object>(request, getHeaders()));

		EnvelopeResponse response = lastResponse.getBody().DeserializeReturnObject(EnvelopeResponse.class);
		System.out.println("Created Envelope: " + response.getEnvelopeID() + " \nFull Response: ");
		print(lastResponse.getBody().getReturnObject());
		System.out.println("Recipient URL: " + getRecipientURL(response.getEnvelopeID()));
	}


	/***
	 * Demonstrate creating an envelope using X/Y Coordinates
	 * @throws Exception
	 */
	public void createEnvelopeFromPDFWithCoordinates() throws Exception
	{
		byte[] doc = loadFile("withoutanchors.pdf");
		Map<String, List<Tab>> tabMap = new HashMap<>();
		List<Tab> tabs = new ArrayList<>();
		Tab tab = new Tab();
		tab.setName("Name");
		tab.setyPosition(320);
		tab.setxPosition(85);
		tab.setPageNumber(1);
		tab.setWidth(150);
		tab.setRequired(true);
		tab.setMaxLength(100);
		tab.setTabLabel("Your Name");
		tabs.add(tab);

		tab = new Tab();
		tab.setName("Address");
		tab.setyPosition(343);
		tab.setxPosition(95);
		tab.setPageNumber(1);
		tab.setWidth(300);
		tab.setRequired(true);
		tab.setMaxLength(150);
		tab.setTabLabel("Your address");
		tabs.add(tab);

		tab = new Tab();
		tab.setName("City");
		tab.setyPosition(365);
		tab.setxPosition(80);
		tab.setPageNumber(1);
		tab.setWidth(200);
		tab.setRequired(true);
		tab.setMaxLength(150);
		tab.setTabLabel("Your city");
		tabs.add(tab);

		tab = new Tab();
		tab.setName("State Abbreviation");
		tab.setyPosition(365);
		tab.setxPosition(280);
		tab.setPageNumber(1);
		tab.setWidth(25);
		tab.setRequired(true);
		tab.setMaxLength(2);
		tab.setTabLabel("Your state");
		tabs.add(tab);

		tab = new Tab();
		tab.setName("Zip");
		tab.setyPosition(365);
		tab.setxPosition(360);
		tab.setPageNumber(1);
		tab.setWidth(50);
		tab.setRequired(true);
		tab.setValidationPattern("^[0-9]{5}$");
		tab.setValidationMessage("Must be five numbers");
		tab.setMaxLength(5);
		tab.setTabLabel("Your postal code");
		tabs.add(tab);

		tabMap.put("textTabs", tabs);

		tabs = new ArrayList<>();
		tab = new Tab();
		tab.setName("Sign Here");
		tab.setyPosition(570);
		tab.setxPosition(105);
		tab.setRequired(true);
		tabs.add(tab);
		tabMap.put("signHereTabs", tabs);

		tabs = new ArrayList<>();
		tab = new Tab();
		tab.setName("Date Signed");
		tab.setyPosition(605);
		tab.setxPosition(350);
		tab.setRequired(true);
		tabs.add(tab);
		tabMap.put("dateSignedTabs", tabs);

		EnvelopeRequest request = new EnvelopeRequest()
				.setEmailRequest(new EmailRequest()
						.setName(name)
						.setRecipientEmail(email)
						.setEmailSubject("Test Document Using Coordinates")
						.setEmailBody("This is a test document")
						.setTest(true)
				)
				.setDocument(doc)
				.setFileName("Loan_Application_" + name.replace(" ", ""))
				.setTabs(tabMap)
				;
		print(new ObjectMapper().valueToTree(request));
		post("/envelope", new HttpEntity<Object>(request, getHeaders()));

		EnvelopeResponse response = lastResponse.getBody().DeserializeReturnObject(EnvelopeResponse.class);
		System.out.println("Created Envelope: " + response.getEnvelopeID() + " \nFull Response: ");
		print(lastResponse.getBody().getReturnObject());
		System.out.println("Recipient URL: " + getRecipientURL(response.getEnvelopeID()));
	}

	/***
	 * Demostrate creating an envelope using a previously defined template
	 * @throws Exception
	 */
	public void createEnvelopeFromTemplate() throws Exception
	{
		TemplateRequest templateRequest = new TemplateRequest()
				.setEmailBody("This is the test message")
				.setEmailSubject("This is from a unit test")
				.setRecipientEmail(email)
				.setRecipientName(name)
				.setTemplateID(templateID)
				.setTest(true);
		post("/envelope/template", new HttpEntity<Object>(templateRequest, getHeaders()));

		EnvelopeResponse response = lastResponse.getBody().DeserializeReturnObject(EnvelopeResponse.class);
		System.out.println("Created Envelope: " + response.getEnvelopeID() + " \nFull Response: ");
		print(lastResponse.getBody().getReturnObject());
		System.out.println("Recipient URL: " + getRecipientURL(response.getEnvelopeID()));
	}


	/***
	 * Demonstrates retrieving the recipient URL for directing the user to the signature page
	 * @param envelopeid
	 * @return A URL
	 * @throws Exception
	 */
	public String getRecipientURL(String envelopeid) throws Exception
	{
		RecipientURLRequest request = new RecipientURLRequest()
				.setEnvelopeID(envelopeid)
				.setRecipientEmail(email)
				.setRecipientName(name)
				.setReturnURL("http://www.loanscience.com")
				.setTest(true);

		print(new ObjectMapper().valueToTree(request));
		post("/envelope/recipienturl", new HttpEntity<Object>(request, getHeaders()));
		return lastResponse.getBody().getReturnObject().toString();
	}


	public void downloadDocument(String envelopeid, Path output) throws Exception
	{
		get("/envelope/" + envelopeid + "/combined", new HttpEntity<>(getHeaders()));
		DownloadDocument downloadDocument = lastResponse.getBody().DeserializeReturnObject(DownloadDocument.class);
		byte[] doc = downloadDocument.getDocument();
		Files.write(output, doc);
	}




	/**
	 * Helper Methods
	 */


	/**
	 * Get Authorization Headers
	 * @return
	 */
	public HttpHeaders getHeaders(){

		HttpHeaders headers = new HttpHeaders();
		headers.add("Authorization", "Basic " + apikey);
		headers.add("Test", "true");
		headers.setAccept(Arrays.asList(MediaType.APPLICATION_JSON));
		return headers;
	}

	public byte[] loadFile(String filename) throws Exception
	{
		Resource resource = new ClassPathResource(filename);
		InputStream is = resource.getInputStream();
		//InputStream is = this.getClass().getResourceAsStream("classpath:" + filename);

		if (is == null)
		{
			throw new Exception("Cannot load document: " + filename);
		}

		ByteArrayOutputStream buffer = new ByteArrayOutputStream();
		int nRead;
		byte[] data = new byte[1024];
		while ((nRead = is.read(data, 0, data.length)) != -1) {
			buffer.write(data, 0, nRead);
		}

		buffer.flush();
		byte[] doc = buffer.toByteArray();

		//ClassLoader classLoader = this.getClass().getClassLoader();
		//Path path = Paths.get(classLoader.getResource(filename).getPath());
		//return Files.readAllBytes(path);
		return doc;
	}

	public ResponseEntity<ServiceResponse> get(String urlSuffix, HttpEntity<?> requestEntity) throws Exception
	{
		return exchange(urlSuffix, HttpMethod.GET, requestEntity);
	}
	public ResponseEntity<ServiceResponse> post(String urlSuffix, HttpEntity<?> requestEntity) throws Exception
	{
		return exchange(urlSuffix, HttpMethod.POST, requestEntity);
	}
	public ResponseEntity<ServiceResponse> put(String urlSuffix, HttpEntity<?> requestEntity) throws Exception
	{
		return exchange(urlSuffix, HttpMethod.PUT, requestEntity);
	}
	public ResponseEntity<ServiceResponse> delete(String urlSuffix, HttpEntity<?> requestEntity) throws Exception
	{
		return exchange(urlSuffix, HttpMethod.DELETE, requestEntity);
	}

	private ResponseEntity<ServiceResponse> lastResponse = null;
	private RestTemplate restTemplate = new RestTemplate();
	public ResponseEntity<ServiceResponse> exchange(String urlSuffix, HttpMethod method, HttpEntity<?> requestEntity) throws Exception
	{
		restTemplate = new RestTemplate();
		lastResponse = null;
		try
		{
			lastResponse = restTemplate.exchange(getURL(urlSuffix), method, requestEntity, ServiceResponse.class);
		}
		catch (HttpServerErrorException e)
		{
			try
			{
				ObjectMapper mapper = new ObjectMapper();
				lastResponse = new ResponseEntity<ServiceResponse>(mapper.readValue(e.getResponseBodyAsString(), ServiceResponse.class), new HttpHeaders(), HttpStatus.BAD_REQUEST);
			}
			catch (Exception e2)
			{
				e2.printStackTrace();
				System.out.println(e.getResponseBodyAsString());
			}

			e.printStackTrace();
		}
		if (lastResponse == null)
		{
			throw new Exception("No response was returned");
		}
		if (lastResponse.hasBody() && lastResponse.getBody() != null)
		{
			print(Json.toJson(lastResponse.getBody()));
		}

		if (lastResponse.getStatusCode() != HttpStatus.OK)
		{
			String error = "";
			if (lastResponse.hasBody() && lastResponse.getBody() != null)
			{
				error = lastResponse.getBody().getError();
			}
			throw new Exception("Request failed. " + error);
		}
		return lastResponse;
	}
	public String getURL(String suffix)
	{
		return baseURL + suffix;
	}
	public static void print(JsonNode json) throws Exception
	{
		ObjectMapper mapper = new ObjectMapper();
		System.out.println(mapper.writerWithDefaultPrettyPrinter().writeValueAsString(json));
	}
}
