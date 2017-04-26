package com.searchemployeeservice.util;

import java.io.IOException;
import java.net.InetAddress;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.transport.InetSocketTransportAddress;
import org.elasticsearch.common.transport.TransportAddress;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.searchemployeeservice.bean.Employee;

public class ElasticSearchUtil {

	private static final String INDEX = "employees";

	private static Client client = null;

	private static Client getClient() throws Exception {
		if (client == null) {
			/*client = NodeBuilder.nodeBuilder()
					.settings(Settings.builder().put("path.home", "/"))
					.client(true).node().client();*/
			
			client = TransportClient
                    .builder()
                    .build()
                    .addTransportAddress(
                                (TransportAddress) new InetSocketTransportAddress(
                                            InetAddress.getByName("127.0.0.1"), 9300));
			
			
			boolean indexExists = client.admin().indices().prepareExists(INDEX)
					.execute().actionGet().isExists();

			if (!indexExists) {
				throw new Exception("Index " + INDEX
						+ " not found in elasticsearch.");
			}

		}
		return client;
	}

	/**
	 * Util method to search the employee in elastic search based on the
	 * employee id given as parameter.
	 * 
	 * @param criteria
	 *            Employee id as search criteria
	 * @return {@link Employee} data as JSON Format
	 * @throws Exception
	 */
	public static List<Employee> searchEmployee(String criteria)
			throws Exception {
		if (client == null) {
			client = getClient();
		}
		SearchResponse idResponse = client.prepareSearch().setIndices(INDEX)
				.setTypes("employee")
				.addFields("empId", "text")
				.addFields("empFirstName", "text")
				.addFields("empLastName", "text")
				.addFields("dob", "date")
				.addFields("city", "text")
				.setQuery(QueryBuilders.matchQuery("empId", criteria))//setQuery(QueryBuilders.wildcardQuery("empFirstName", "*"+criteria + "*"))
				.execute().actionGet();

		List<Employee> employeeList = new ArrayList<>();
		for (SearchHit hit : idResponse.getHits()) {
			Employee emp = new Employee();

			emp.setEmpId((String) hit.field("empId").getValue());
			emp.setEmpFirstName((String) hit.field("empFirstName").getValue());
			emp.setEmpLastName((String) hit.field("empLastName").getValue());
			emp.setCity((String) hit.field("city").getValue());

			SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
			Date date = sdf.parse((String) hit.field("dob").getValue());
			emp.setDob(date);

			employeeList.add(emp);
		}

		//String result = convertEmployeesIntoJSonData(employeeList);

		return employeeList;
	}

	private static String convertEmployeesIntoJSonData(List<Employee> employees) {
		ObjectMapper mapper = new ObjectMapper();

		StringBuffer empVal = new StringBuffer();
		for (Employee emp : employees) {

			try {
				// Plain JSON
				empVal.append(mapper.writeValueAsString(emp));

			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		return empVal.toString();
	}

	/**
	 * Util method to convert JSON employee data into {@link Employee} object
	 * 
	 * @param jSonData
	 *            Employee data as String
	 * @return {@link Employee} object
	 * @throws JsonParseException
	 * @throws JsonMappingException
	 * @throws IOException
	 */
	public static Employee convertJSonDataToEmployees(String jSonData)
			throws JsonParseException, JsonMappingException, IOException {
		ObjectMapper mapper = new ObjectMapper();

		Employee emp = mapper.readValue(jSonData, Employee.class);

		return emp;
	}

	/**
	 * Method to save the employee data into Elastic Search database
	 * 
	 * @param employee
	 *            {@link Employee} object data
	 * @return status of the operation as success/failure
	 * @throws Exception
	 */
	public static String saveEmployee(Employee employee) throws Exception {
		if (client == null) {
			client = getClient();
		}

		IndexResponse response = client
				.prepareIndex(INDEX, "employee", employee.getEmpId())
				.setSource(createJsonDocument(employee)).execute().actionGet();

		if (response.getId() != null) {
			return "Success";
		}
		return "failure";
	}

	/**
	 * Method to create {@link Map} of JSON fields and values for the
	 * {@link Employee} object
	 * 
	 * @param emp
	 *            {@link Employee} data
	 * @return {@link Map} of JSON fields and values
	 */
	public static Map<String, Object> createJsonDocument(Employee emp) {
		Map<String, Object> jsonDocument = new HashMap<String, Object>();
		jsonDocument.put("empId", emp.getEmpId());
		jsonDocument.put("empFirstName", emp.getEmpFirstName());
		jsonDocument.put("empLastName", emp.getEmpLastName());
		jsonDocument.put("city", emp.getCity());
		jsonDocument.put("dob", emp.getDob());
		return jsonDocument;
	}
}
