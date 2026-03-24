package motorphpayrollsystem;

import java.util.Scanner;

/**
 * MotorPHPayrollSystem.java
 *
 * This is the main entry point of the MotorPH Payroll System.
 * Displays the system menu and allows the user to access
 * different payroll features such as viewing employee
 * information, computing salaries, and computing net pay.
 *
 * Access rules:
 * - Options 1 to 3 are for payroll staff only.
 * - Option 4 is for employees who want to view their own details.
 * - Option 5 closes the program.
 */
public class MotorPHPayrollSystem {

    // ==============================
    // CONSTANTS (used across system)
    // ==============================
    public static final int MAX_EMPLOYEES = 100;
    public static final String EMPLOYEE_FILE = "EmployeeData/EmployeeDetails.csv";
    public static final String ATTENDANCE_FILE = "EmployeeData/AttendanceRecord.csv";

    /**
     * Computes worked hours based on login and logout time.
     * Rules applied:
     * - official work hours only: 8:00 AM to 5:00 PM
     * - 10-minute grace period
     * - 1-hour unpaid break
     * - maximum of 8 hours per day
     */
    public static double computeWorkedHours(String logInTime, String logOutTime) {

        int loginMinutes = MotorPHPayrollSystem.convertTimeToMinutes(logInTime);
        int logoutMinutes = MotorPHPayrollSystem.convertTimeToMinutes(logOutTime);

        int officialStart = 8 * 60; // 8:00 AM
        int officialEnd = 17 * 60;  // 5:00 PM

        int countedStart;

        // Apply grace period: 8:00 AM to 8:10 AM is counted as 8:00 AM
        if (loginMinutes <= ((8 * 60) + 10)) {
            countedStart = officialStart;
        } else {
            countedStart = loginMinutes;
        }

        // Do not count work before official shift start
        if (countedStart < officialStart) {
            countedStart = officialStart;
        }

        int countedEnd = logoutMinutes;

        // Do not count work after official shift end
        if (countedEnd > officialEnd) {
            countedEnd = officialEnd;
        }

        int workedMinutes = countedEnd - countedStart;

        // Deduct 1 hour unpaid break if there is valid worked time
        if (workedMinutes > 0) {
            workedMinutes -= 60;
        }

        // Prevent negative worked time
        if (workedMinutes < 0) {
            workedMinutes = 0;
        }

        double workedHours = workedMinutes / 60.0;

        // Prevent counted work hours from exceeding 8 hours in one day
        workedHours = Math.min(workedHours, 8.0);

        return workedHours;
    }

    /**
     * Displays the main menu of the system.
     */
    public static void displayMenu() {
        System.out.println();
        System.out.println("==================================================");
        System.out.println("              MOTORPH PAYROLL SYSTEM              ");
        System.out.println("==================================================");
        System.out.println("1 - Calculate Hours Worked");
        System.out.println("2 - Compute Semi-Monthly Salary");
        System.out.println("3 - Compute Net Pay");
        System.out.println("4 - View Employee Details");
        System.out.println("5 - Exit");
        System.out.println("==================================================");
    }

    /**
     * Reads a whole-number input and checks if it is within the allowed range.
     */
    public static int readValidIntInRange(Scanner input, String prompt, String errorMessage, int min, int max) {
        while (true) {
            System.out.print(prompt);
            String line = input.nextLine().trim();

            try {
                int value = Integer.parseInt(line);

                if (value >= min && value <= max) {
                    return value;
                } else {
                    System.out.println(errorMessage);
                }
            } catch (NumberFormatException e) {
                System.out.println("Invalid input. Please enter a whole number only.");
            }
        }
    }

    /**
     * Validates the payroll staff login before restricted features are opened.
     */
    public static boolean payrollStaffLogin(Scanner input) {
        System.out.println();
        System.out.println("--------------- PAYROLL STAFF LOGIN ---------------");
        System.out.println("Type 0 as username to go back to the main menu.");
        System.out.print("Enter username: ");
        String username = input.nextLine().trim();

        // Check if user entered "0" to cancel login and go back
        if (username.equals("0")) {
            System.out.println("Returning to main menu...");
            return false; // stop login process and return to menu
        }

        System.out.print("Enter password: ");
        String password = input.nextLine().trim();

        // Check if both username and password match the required credentials
        if (username.equals("payroll_staff") && password.equals("12345")) {
            System.out.println("Login successful.");
            return true; // allow access
        } else {
            System.out.println("Invalid username or password.");
            return false; // deny access
        }
    }

    /**
     * Converts a time string in HH:MM format into total minutes.
     */
    public static int convertTimeToMinutes(String time) {
        String[] timeParts = time.split(":");
        int hour = Integer.parseInt(timeParts[0]);
        int minute = Integer.parseInt(timeParts[1]);
        return (hour * 60) + minute;
    }

    /**
     * Main method of the program.
     * It keeps showing the menu until the user selects Exit.
     */
    public static void main(String[] args) {
        Scanner input = new Scanner(System.in);
        int option = 0;

         // Loop runs until user selects option 5 (Exit)
        while (option != 5) {
            displayMenu();

            option = readValidIntInRange(
                    input,
                    "Select option: ",
                    "Invalid option. Please select 1 to 5 only.",
                    1,
                    5
            );

            // Execute action based on selected option
            switch (option) {
                case 1: // Only payroll staff can access this feature
                    if (payrollStaffLogin(input)) {
                        System.out.println();
                        System.out.println("Opening CalculateHoursWorked...");
                        CalculateHoursWorked.run(input);
                    }
                    break;

                case 2: // Payroll staff login required
                    if (payrollStaffLogin(input)) {
                        System.out.println();
                        System.out.println("Opening ComputeSemiMonthlySalary...");
                        ComputeSemiMonthlySalary.run(input);
                    }
                    break;

                case 3: // Payroll staff login required
                    if (payrollStaffLogin(input)) {
                        System.out.println();
                        System.out.println("Opening ComputeNetPay...");
                        ComputeNetPay.run(input);
                    }
                    break;

                case 4: // Employees can view their details 
                    System.out.println();
                    System.out.println("Opening ViewEmployeeDetails...");
                    ViewEmployeeDetails.run(input);
                    break;

                case 5: // Exit the program
                    System.out.println("Exiting MotorPH Payroll System.");
                    break;

                default: // Safety check 
                    System.out.println("Invalid option. Please select 1 to 5 only.");
            }
        }

        input.close();
    }
}