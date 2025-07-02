package com.mindex.challenge.controller;

import com.mindex.challenge.data.Compensation;
import com.mindex.challenge.service.EmployeeService;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.time.LocalDate;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.any;

@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class EmployeeControllerTest {
    //Mockito tests classes on an individual basis, as a result since it was used with the compensation endpoints,
    //an additional test class is needed for those controller endpoints

    @Mock
    private EmployeeService mockEmployeeService;

    @InjectMocks
    private EmployeeController employeeController = new EmployeeController();

    @Before
    public void setup() {
        mockEmployeeService = Mockito.mock(EmployeeService.class);
        MockitoAnnotations.openMocks(this);
    }

    @Test
    public void testSubmitCompensation() {
        Mockito.when(mockEmployeeService.submitCompensation(any(String.class), any(Compensation.class))).thenReturn(createTestCompensation());

        Compensation test = employeeController.submitCompensation("1", new Compensation());

        assertNotNull(test);
        assertEquals("12345", test.getEmployeeCompensationId());
        assertEquals("$123,456.30", test.getSalary());
        assertEquals(LocalDate.parse("2025-08-24"), test.getEffectiveDate());
    }

    @Test
    public void testReadCompensation() {
        Mockito.when(mockEmployeeService.readCompensation(any(String.class))).thenReturn(createTestCompensation());

        Compensation test = employeeController.readCompensation("1");

        assertNotNull(test);
        assertEquals("12345", test.getEmployeeCompensationId());
        assertEquals("$123,456.30", test.getSalary());
        assertEquals(LocalDate.parse("2025-08-24"), test.getEffectiveDate());
    }

    private static Compensation createTestCompensation() {
        Compensation testCompensation = new Compensation();
        testCompensation.setEmployeeCompensationId("12345");
        testCompensation.setSalary("123456.3");
        testCompensation.setEffectiveDate(LocalDate.parse("2025-08-24"));
        return testCompensation;
    }
}
