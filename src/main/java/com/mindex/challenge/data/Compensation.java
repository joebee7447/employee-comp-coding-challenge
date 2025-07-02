package com.mindex.challenge.data;

import java.text.NumberFormat;
import java.time.LocalDate;

public class Compensation {
    String employeeCompensationId;
    String salary;
    LocalDate effectiveDate;

    public Compensation() {

    }

    public String getEmployeeCompensationId() {
        return employeeCompensationId;
    }

    public void setEmployeeCompensationId(String employeeCompensationId) {
        this.employeeCompensationId = employeeCompensationId;
    }

    public String getSalary() {
        return salary;
    }

    public void setSalary(String salary) {
        this.salary = NumberFormat.getCurrencyInstance().format(Double.valueOf(salary));
    }

    public LocalDate getEffectiveDate() {
        return effectiveDate;
    }

    public void setEffectiveDate(LocalDate effectiveDate) {
        this.effectiveDate = effectiveDate;
    }
}
