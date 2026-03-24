Team Details:
Rizalyn C. Novales

Program Details: 

MotorPH Payroll System

MotorPH Payroll System is a console-based Java application
that simulates a basic payroll management system for the fictional
company MotorPH. The program reads employee and payroll data from CSV
files and performs payroll computations.

Features:
- View employee information
- Calculate hours worked
- Compute semi-monthly salary (gross salary)
- Compute net pay with government deductions (SSS,PhilHealth, Pag-IBIG, withholding tax) 
- Simple console menu navigation

Technologies Used - Java - NetBeans IDE - CSV files for data storage

Project Structure 
MotorPHPayrollSystem.java - Main menu controller 
ViewEmployeeDetails.java - Displays employee information 
CalculateHoursWorked.java - Computes hours worked 
ComputeSemiMonthlySalary.java - Computes semi-monthly salary 
ComputeNetPay.java - Computes net pay and deductions

How to Run 
1. Open the project in NetBeans or any Java IDE. 
2. Ensure all CSV data files are in the correct directory. 
3. Run MotorPHPayrollSystem.java. 
4. Use the console menu to access payroll features.


Project Plan Link:
https://docs.google.com/spreadsheets/d/1xD3NUfLA_d_U8XaIDkspsaAhGg6THhWJ7DUnp40U6vU/edit?usp=sharing


Update Note
March 17, 2026

The repository was updated to reflect the original MotorPH Employee Data. 
The employee records were revised to match the official dataset provided in the MotorPH requirements.


Update Note
March 18, 2026

The repository was updated to resolve merge conflict and retain non-rounded payroll output formatting across modules.
Updated PhilHealth calculation logic

Update Note
March 24, 2026
The repository was update to reflect the following improvements:
- Renamed variables to more descriptive names and added inline comments to clarify logic
- Replaced hardcoded values with constants for file paths and system limits
- Updated file handling to use try-with-resources for safer and cleaner implementation
- Identified repeated worked-hours computation logic and created a reusable computeWorkedHours() method in MotorPHPayrollSystem, then replaced duplicated blocks in all modules
- Moved shared logic (worked-hours computation) into the main system class and updated all files to call this method instead of duplicating code
- Standardized naming across files using clear and consistent variable names
- Added validation to prevent negative values and ensured proper limits on work hours
- Replaced direct input parsing with validated methods (readValidIntWithBack, readPositiveIntWithBack) to handle invalid inputs and prevent runtime errors




