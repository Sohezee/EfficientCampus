# EfficientCampus <img src="src/main/resources/static/logo192.png" width="70" height="70" align="center">

EfficientCampus is a full-stack, open-source application built with **Spring** to automatically sign up users for their preferred Ac-Lab sessions offered in certain RSDMO high schools.

You can access its deployment [here](https://www.efficientcampus.org)

## **Features**
- User-friendly interface for preference input
- Secure data storage with AES encryption
- Built-in user authentication and session management

## **Technology Stack**
- **Backend**: Spring, Hibernate, MySQL
- **Frontend**: React
- **Automation**: Playwright
- **HTTP Client**: OkHttp

## **Getting Started**

To run this program locally, follow these steps:

### **1. Clone the Repository**

First, clone the backend repository for this project:

```bash
git clone https://github.com/yourusername/EfficientCampusBackend.git
cd EfficientCampusBackend
```

Then follow the steps below to set up both the backend and frontend components.

---

### **2. Prerequisites**

Ensure you have the following installed:
- [Java 17+](https://www.oracle.com/java/technologies/javase-jdk17-downloads.html)
- [Maven](https://maven.apache.org/install.html)
- [MySQL](https://dev.mysql.com/downloads/installer/)
- [Node.js](https://nodejs.org/en/) (for the React frontend)

---

### **Step-by-Step Setup**

#### **3. Set Up MySQL Database**

Create and configure your MySQL database by running the following SQL script:

```sql
CREATE DATABASE IF NOT EXISTS `user_directory`;
USE `user_directory`;

DROP TABLE IF EXISTS `user`;

CREATE TABLE `user` (
  `id` int NOT NULL AUTO_INCREMENT,
  `email` varchar(45) DEFAULT NULL,
  `password` varchar(45) DEFAULT NULL,
  `offering_name_one` varchar(65) DEFAULT NULL,
  `teacher_display_one` varchar(65) DEFAULT NULL,
  `offering_name_two` varchar(65) DEFAULT NULL,
  `teacher_display_two` varchar(65) DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=latin1;
```

#### **4. Configure Application Properties**

Create an `application.properties` file in the `src/main/resources` directory and configure the following settings:

```properties
# Database configuration
spring.datasource.url=jdbc:mysql://localhost:3306/user_directory
spring.datasource.username=YOUR_USERNAME
spring.datasource.password=YOUR_PASSWORD


# Encryption key for sensitive data
encryption.secret.key=YOUR_256_BIT_AES_KEY
```

> **Note:**
> - Replace `YOUR_USERNAME`, `YOUR_PASSWORD`, and `YOUR_256_BIT_AES_KEY` with the actual values.
> - Ensure that your encryption key is exactly 256 bits (32 characters).


#### **6. Build and Run the Application**

To build and run the application, navigate to the project directory and execute:

```bash
mvn clean install
mvn spring-boot:run
```

Alternatively, you can build a JAR file and run it:

```bash
mvn clean package
java -jar target/demo-0.0.1-SNAPSHOT.jar
```

---

### **Frontend Setup (React)**

The frontend currently located in the `src/main/resources/static` directory is configured to 
make backend API calls to `https://efficientcampus.org`. However, when running the backend locally, 
you will need to update these API calls to point to your local server.

To do this, follow these steps:

1. Clone the `EfficientCampusFrontend` repository from my GitHub account and navigate to the project directory:

    ```bash
    git clone https://github.com/yourusername/EfficientCampusFrontend.git
    cd EfficientCampusFrontend
    ```

2. **Install dependencies**:
   ```bash
   npm install
   ```

3. **Update API URLs**:
   Change all API calls from `https://efficientcampus.org/api` to `http://localhost:8080/api`.  
   *Note: Only the `Home.js` and `Login.js` files contain API calls.*

4. **Build the project**:
   ```bash
   npm run build
   ```

5. **Copy build files**:
   Copy the contents of the newly generated `build` directory into the `src/main/resources/static` directory of the backend project, replacing the current contents.

6. **Rebuild as JAR**:
   If you want to package the project as a JAR, rebuild it with:
   ```bash
   mvn clean package
   java -jar target/demo-0.0.1-SNAPSHOT.jar
   ```

The application will be available at `http://localhost:8080`.

---

### **Security Considerations**

- Make sure to store your API keys and encryption keys securely. Never expose them publicly or in version control.
- Use strong passwords for the MySQL user account.

---

### **Contributing**

If you would like to contribute to this project, feel free to submit a pull request. Please make sure to follow the project's coding standards.

---

### **License**

This project is open source under the [MIT License](https://opensource.org/licenses/MIT).

---

### **Author**

**Soham Vij**  
Feel free to contact me at [svij024@gmail.com](mailto:svij024@gmail.com) for any questions or support.