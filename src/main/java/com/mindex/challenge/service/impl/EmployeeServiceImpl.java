package com.mindex.challenge.service.impl;

import com.mindex.challenge.dao.CompensationRepository;
import com.mindex.challenge.dao.EmployeeRepository;
import com.mindex.challenge.data.Compensation;
import com.mindex.challenge.data.Employee;
import com.mindex.challenge.data.ReportingStructure;
import com.mindex.challenge.service.EmployeeService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.UUID;

@Service
public class EmployeeServiceImpl implements EmployeeService {

    private static final Logger LOG = LoggerFactory.getLogger(EmployeeServiceImpl.class);

    @Autowired
    private EmployeeRepository employeeRepository;
    @Autowired
    private CompensationRepository compensationRepository;

    @Override
    public Employee create(Employee employee) {
        LOG.debug("Creating employee [{}]", employee);

        employee.setEmployeeId(UUID.randomUUID().toString());
        employeeRepository.insert(employee);

        return employee;
    }

    @Override
    public Employee read(String id) {
        //updated logger message, previously said 'creating'
        LOG.debug("Reading employee with id [{}]", id);

        Employee employee = employeeRepository.findByEmployeeId(id);

        if (employee == null) {
            throw new RuntimeException("Invalid employeeId: " + id);
        }

        return employee;
    }

    @Override
    public Employee update(Employee employee) {
        LOG.debug("Updating employee [{}]", employee);

        //added a delete method, was getting an error on updating then reading from same ID on the existing code
        //new functionality, it  deletes the old employee from the DB, then adds in the new one
        employeeRepository.deleteByEmployeeId(employee.getEmployeeId());

        return employeeRepository.save(employee);
    }

    @Override
    public ReportingStructure reports(String id) {
        LOG.debug("Generating number of reports for employee with id [{}]", id);

        Employee reportingEmployee = employeeRepository.findByEmployeeId(id);

        if (reportingEmployee == null) {
            throw new RuntimeException("Invalid employeeId: " + id);
        }

        //assigning the generated number of reports to its own method, due to utilizing recursion to increment effectively
        ReportingStructure reportingStructure = new ReportingStructure(reportingEmployee, generateNumberOfReports(reportingEmployee, 0));

        return reportingStructure;
    }

    @Override
    public Compensation submitCompensation(String id, Compensation compensation) {
        LOG.debug("Creating compensation for employee with id [{}]", id);

        //Made the decision to migrate all compensation info to its own table/repository. this is because it is handled entirely separately/isnt dependent on any other employee info
        //the compensation set/update method written here would be rendered obsolete, because we could just use the standard /update method to update the compensation info with the new employee payload
        //using this way, we completely isolate all of the compensation info to its own space, rather than adding the compensation to the existing Employee object
        //as if there are 2 tables in the DB for compensation and employee data each, and the employee id is the foreign key

        compensation.setEmployeeCompensationId(id);

        //checking to see if employee exists before creating/updating compensation data
        if (employeeRepository.findByEmployeeId(id) == null) {
            throw new RuntimeException("Invalid employeeId: " + id);
        }

        Compensation employeeCompensation = compensationRepository.findByEmployeeCompensationId(id);

        //if compensation info does not exist for this user, create it
        if (employeeCompensation == null) {
            return compensationRepository.insert(compensation);
        }

        //if it exists, update the info. I am using the updated /update method that contains the fix mentioned above in the method
        compensationRepository.deleteByEmployeeCompensationId(compensation.getEmployeeCompensationId());

        return compensationRepository.save(compensation);

    }

    @Override
    public Compensation readCompensation(String id) {
        LOG.debug("Reading compensation for employee with id [{}]", id);

        //checking to see if employee exists, returns invalid employee message
        if (employeeRepository.findByEmployeeId(id) == null) {
            throw new RuntimeException("Invalid employeeId: " + id);
        }

        Compensation employeeCompensation = compensationRepository.findByEmployeeCompensationId(id);

        //checking to see if employee compensation exists, returns no compensation found for this employee message
        if (employeeCompensation == null) {
            throw new RuntimeException("No compensation data found for employeeId: " + id);
        }

        return employeeCompensation;
    }

    private Integer generateNumberOfReports(Employee employee, Integer numberOfReports) {

        //once we reach an employee with no direct reports, we return out of the recursion
        if(employee.getDirectReports() == null) {
            return numberOfReports;
        }

        for(int i = 0; i < employee.getDirectReports().size(); i++) {

            //in the existing database, the directReport employee objects are blank, only containing employee ID
            //by doing this call below, we can fill in those extra variables, most notably, the direct reports object within employee
            employee.getDirectReports().set(i, employeeRepository.findByEmployeeId(employee.getDirectReports().get(i).getEmployeeId()));

            //increment the number of reports value and then recursively call the generateNumberOfReports method with incremented value, and each directReport employee
            numberOfReports++;
            numberOfReports = generateNumberOfReports(employee.getDirectReports().get(i), numberOfReports);
        }
        return numberOfReports;
    }
}
