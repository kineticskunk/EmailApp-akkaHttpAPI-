    import akka.event.Logging;
    import akka.event.LoggingAdapter;
    import akka.actor.*;

    import java.util.ArrayList;
    import java.util.List;
    import java.util.Scanner;

    public class EmailActor extends AbstractActor {
        LoggingAdapter log = Logging.getLogger(getContext().getSystem(), this);

    public static class User {
        private String firstName;
        private String lastName;
        private String password;
        private String department;
        private String email;
        private int mailboxCapacity = 500;
        private int defaultPasswordLength = 10;
        private String alternateEmail;
        private String companySuffix = "aeycompany.com";

        //Constructor to receive the first name and last name
        public User(String firstName, String lastName, String department) {
            this.firstName = firstName;
            this.lastName = lastName;
            System.out.println("Email Created" + this.firstName + "" + this.lastName);
            // Call a method asking for the department - return the department
            this.department = setDepartment();
            System.out.println("Department: " + this.department);
            //Call a method to returns a random password
            this.password = randomPassword(defaultPasswordLength);
            System.out.println("Your password is: " + this.password);

            //Combine elements to generate email
            email = firstName.toLowerCase() + "." + lastName.toLowerCase() + "@" + department + "." + companySuffix;
            System.out.println("Your email is:" + email);
        }


        // Ask for the department

        private String setDepartment() {
            System.out.println("New Worker: " + firstName + ". DEPARTMENT CODES:\n1 for Sales\n2 for Development\n3 for Accounting\n0 for none\nEnter department code: ");
            Scanner in = new Scanner(System.in);
            int depChoice = in.nextInt();
            if (depChoice == 1) {
                return "sales";
            } else if (depChoice == 2) {
                return "dev";
            } else if (depChoice == 3) {
                return "acct";
            } else {
                return "";
            }
        }

        // Generate a random password
        private String randomPassword(int length) {
            String passwordSet = "ABCDEFGHIJKLMNOPQRSTUWXYZ0123456789!@#$%";
            char[] password = new char[length];
            for (int i = 0; i < length; i++) {
                int rand = (int) (Math.random() * passwordSet.length());
                password[i] = passwordSet.charAt(rand);
            }
            return new String(password);
        }

        // Set the mailbox capacity
        public void setMailboxCapacity(int capacity) {
            this.mailboxCapacity = capacity;
        }


        // Set the alternate email
        public void setAlternateEmail(String altEmail) {
            this.alternateEmail = altEmail;

        }

        // Change the password
        public void changePassword(String password) {
            this.password = password;
        }

        public int getMailboxCapacity() {

            return mailboxCapacity;
        }

        public String getAlternateEmail() {
            return alternateEmail;
        }

        public String getPassword() {
            return password;
        }

        public String getName() {
            return firstName;
        }
        public String showInfo() {
            return "Display NAME: " + firstName + " " + lastName +
                    "\nCOMPANY EMAIL: " + email +
                    "\nMAILBOX CAPACITY: " + mailboxCapacity + "mb";
        }


    }

        public static class Users{
            private final List<User> users;

            public Users() {
                this.users = new ArrayList<>();
            }

            public Users(List<User> users) {
                this.users = users;
            }

            public List<User> getUsers() {

                return users;
            }
        }
//#user-case-classes

        static Props props() {

            return Props.create(EmailActor.class);
        }

        private final List<User> users = new ArrayList<>();

        @Override
        public Receive createReceive(){
            return receiveBuilder()
                    .match(emailMessages.GetUsers.class, getUsers -> getSender().tell(new Users(users),getSelf()))
                    .match(emailMessages.CreateUser.class, createUser -> {
                        users.add(createUser.getUser());
                        getSender().tell(new emailMessages.ActionPerformed(
                                String.format("User %s created.", createUser.getUser().getName())),getSelf());
                    })
                    .match(emailMessages.GetUser.class, getUser -> {
                        getSender().tell(users.stream()
                                .filter(user -> user.getName().equals(getUser.getName()))
                                .findFirst(), getSelf());
                    })
                    .match(emailMessages.DeleteUser.class, deleteUser -> {
                        users.removeIf(user -> user.getName().equals(deleteUser.getName()));
                        getSender().tell(new emailMessages.ActionPerformed(String.format("User %s deleted.", deleteUser.getName())),
                                getSelf());

                    }).matchAny(o -> log.info("received unknown message"))
                    .build();
        }
    }


