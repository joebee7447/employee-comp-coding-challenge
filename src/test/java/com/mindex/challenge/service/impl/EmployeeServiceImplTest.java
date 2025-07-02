package com.mindex.challenge.service.impl;

import com.mindex.challenge.dao.CompensationRepository;
import com.mindex.challenge.dao.EmployeeRepository;
import com.mindex.challenge.data.Compensation;
import com.mindex.challenge.data.Employee;
import com.mindex.challenge.data.ReportingStructure;
import com.mindex.challenge.service.EmployeeService;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit4.SpringRunner;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotEquals;
import static org.mockito.ArgumentMatchers.any;

@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class EmployeeServiceImplTest {

    private String employeeUrl;
    private String employeeIdUrl;
    private String employeeReportingStructureUrl;

    @Mock
    private EmployeeRepository mockEmployeeRepository;
    @Mock
    private CompensationRepository mockCompensationRepository;

    //didn't remove existing test service
    @Autowired
    private EmployeeService employeeService;

    //added additional one with mockito included
    @InjectMocks
    private EmployeeService testEmployeeService = new EmployeeServiceImpl();

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Before
    public void setup() {
        employeeUrl = "http://localhost:" + port + "/employee";
        employeeIdUrl = "http://localhost:" + port + "/employee/{id}";
        employeeReportingStructureUrl = "http://localhost:" + port + "/reporting/{id}";

        //utilizing mockito for compensation implementation
        mockEmployeeRepository = Mockito.mock(EmployeeRepository.class);
        mockCompensationRepository = Mockito.mock(CompensationRepository.class);

        MockitoAnnotations.openMocks(this);
    }

    @Test
    public void testCreateReadUpdate() {
        Employee testEmployee = new Employee();
        testEmployee.setFirstName("John");
        testEmployee.setLastName("Doe");
        testEmployee.setDepartment("Engineering");
        testEmployee.setPosition("Developer");

        // Create checks
        Employee createdEmployee = restTemplate.postForEntity(employeeUrl, testEmployee, Employee.class).getBody();

        assertNotNull(createdEmployee.getEmployeeId());
        assertEmployeeEquivalence(testEmployee, createdEmployee);


        // Read checks
        Employee readEmployee = restTemplate.getForEntity(employeeIdUrl, Employee.class, createdEmployee.getEmployeeId()).getBody();
        assertEquals(createdEmployee.getEmployeeId(), readEmployee.getEmployeeId());
        assertEmployeeEquivalence(createdEmployee, readEmployee);


        // Update checks
        readEmployee.setPosition("Development Manager");

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        Employee updatedEmployee =
                restTemplate.exchange(employeeIdUrl,
                        HttpMethod.PUT,
                        new HttpEntity<Employee>(readEmployee, headers),
                        Employee.class,
                        readEmployee.getEmployeeId()).getBody();

        assertEmployeeEquivalence(readEmployee, updatedEmployee);
    }

    @Test(expected = RuntimeException.class)
    public void testReadEmployeeNotFound() {
        testEmployeeService.read("1");
    }

    @Test
    public void testReports () {

        //create 3 test employees and link together using direct reports
        Employee testEmployee1 = new Employee();

        Employee testEmployee2 = new Employee();
        List<Employee> testEmployee2List = new ArrayList<>();
        testEmployee2List.add(restTemplate.postForEntity(employeeUrl, testEmployee1, Employee.class).getBody());
        testEmployee2.setDirectReports(testEmployee2List);

        Employee testEmployee3 = new Employee();
        List<Employee> testEmployee3List = new ArrayList<>();
        testEmployee3List.add(restTemplate.postForEntity(employeeUrl, testEmployee2, Employee.class).getBody());
        testEmployee3.setDirectReports(testEmployee3List);

        //grab test id when posting the parent employee to the db
        String testId = restTemplate.postForEntity(employeeUrl, testEmployee3, Employee.class).getBody().getEmployeeId();

        //get reporting structure based on the test id grabbed above
        ReportingStructure test = restTemplate.getForEntity(employeeReportingStructureUrl, ReportingStructure.class, testId).getBody();

        //test for null value first, then expected value
        assertNotNull(test);
        assertEquals("2", test.getNumberOfReports().toString());
    }

    @Test(expected = RuntimeException.class)
    public void testReportsEmployeeNotFound() {
        testEmployeeService.reports("1");
    }

    @Test(expected = RuntimeException.class)
    public void testSubmitCompensationEmployeeNotFound() {
        Compensation testCompensation = createTestCompensation();
        testEmployeeService.submitCompensation("1", testCompensation);
    }

    @Test
    public void testSubmitCompensationCreateAndUpdate() {
        Compensation testCompensation = createTestCompensation();
        Mockito.when(mockEmployeeRepository.findByEmployeeId(any())).thenReturn(new Employee());
        Mockito.when(mockCompensationRepository.insert(any(Compensation.class))).thenReturn(createTestCompensation());
        Compensation testInserted = testEmployeeService.submitCompensation("12345", testCompensation);

        assertNotNull(testInserted);
        assertEquals("12345", testInserted.getEmployeeCompensationId());
        assertEquals(testCompensation.getEffectiveDate(), testInserted.getEffectiveDate());
        assertEquals(testCompensation.getSalary(), testInserted.getSalary());
        Mockito.verify(mockCompensationRepository, Mockito.times(1)).insert(any(Compensation.class));

        Compensation updatedCompensation = createTestCompensation();
        updatedCompensation.setSalary("987654321");
        updatedCompensation.setEmployeeCompensationId("54321");

        Mockito.when(mockCompensationRepository.findByEmployeeCompensationId(any(String.class))).thenReturn(updatedCompensation);
        Mockito.when(mockCompensationRepository.save(any(Compensation.class))).thenReturn(updatedCompensation);
        Compensation testUpdated = testEmployeeService.submitCompensation("54321", updatedCompensation);

        assertNotNull(testUpdated);
        assertNotEquals(testUpdated.getSalary(), testCompensation.getSalary());
        Mockito.verify(mockCompensationRepository, Mockito.times(1)).deleteByEmployeeCompensationId(any(String.class));
        Mockito.verify(mockCompensationRepository, Mockito.times(1)).save(any(Compensation.class));
    }

    @Test
    public void testReadCompensation() {
        Mockito.when(mockEmployeeRepository.findByEmployeeId(any())).thenReturn(new Employee());
        Mockito.when(mockCompensationRepository.findByEmployeeCompensationId(any())).thenReturn(createTestCompensation());

        Compensation test = testEmployeeService.readCompensation("1");

        assertNotNull(test);
        assertEquals("12345", test.getEmployeeCompensationId());
        assertEquals("$123,456.30", test.getSalary());
        assertEquals(LocalDate.parse("2025-08-24"), test.getEffectiveDate());
    }

    @Test(expected = RuntimeException.class)
    public void testReadCompensationEmployeeNotFound() {
        testEmployeeService.readCompensation("1");
    }

    @Test(expected = RuntimeException.class)
    public void testReadCompensationDataNotFound() {
        Mockito.when(mockEmployeeRepository.findByEmployeeId(any())).thenReturn(new Employee());
        testEmployeeService.readCompensation("1");
    }

    private static void assertEmployeeEquivalence(Employee expected, Employee actual) {
        assertEquals(expected.getFirstName(), actual.getFirstName());
        assertEquals(expected.getLastName(), actual.getLastName());
        assertEquals(expected.getDepartment(), actual.getDepartment());
        assertEquals(expected.getPosition(), actual.getPosition());
    }

    private static Compensation createTestCompensation() {
        Compensation testCompensation = new Compensation();
        testCompensation.setEmployeeCompensationId("12345");
        testCompensation.setSalary("123456.3");
        testCompensation.setEffectiveDate(LocalDate.parse("2025-08-24"));
        return testCompensation;
    }
}
