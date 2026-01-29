# 🎯 AI-Powered Resume Screening API

An intelligent resume screening system built with Spring Boot that uses Meta's Llama model to automatically evaluate candidate resumes against job requirements.

## 🚀 Features

- **Role-Based Authentication**: Separate access for Recruiters and Candidates
- **AI-Powered Resume Parsing**: Extract structured data from PDF/DOCX resumes
- **Intelligent Screening**: Match candidates to job postings with detailed scoring
- **Real-Time Caching**: Redis-based caching for optimal performance
- **Cloud Storage**: AWS S3 integration for secure file storage
- **RESTful API**: Well-documented API endpoints
- **Rate Limiting**: Prevent API abuse with intelligent rate limiting

## 🛠️ Tech Stack

- **Backend**: Spring Boot 4.0.1, Java 21
- **Database**: PostgreSQL 15+
- **Cache**: Redis 7+
- **Storage**: AWS S3
- **AI**: meta-llama/llama-3.3-70b-instruct:free
- **Security**: Spring Security + JWT
- **Build Tool**: Maven

## 📋 Prerequisites

- Java 21 or higher
- PostgreSQL 15+
- Redis 7+
- AWS Account (Free Tier)
- OpenAI API Key
- Maven 3.8+

## ⚙️ Setup Instructions

### 1. Clone the Repository
```bash
git clone https://github.com/barveakshat/AI-resume-screening-api.git
cd resume-screening-api
```

### 2. Database Setup
```bash
# Create PostgreSQL database
psql -U postgres
CREATE DATABASE resume_screening_db;
```

Run the SQL scripts from `src/main/resources/db/schema.sql` to create tables.

### 3. Environment Configuration

Copy the example environment file:
```bash
cp .env.example .env
```

Edit `.env` and add your credentials:
```env
AWS_ACCESS_KEY_ID=your_key
AWS_SECRET_ACCESS_KEY=your_secret
OPENAI_API_KEY=your_openai_key
# ... etc
```

Copy and configure application properties:
```bash
cp src/main/resources/application-dev.yml.example src/main/resources/application-dev.yml
```

### 4. Install Dependencies
```bash
mvn clean install
```

### 5. Run the Application
```bash
mvn spring-boot:run
```

The API will be available at: `http://localhost:8080`

## 📚 API Documentation

Once running, access Swagger UI at:
```
http://localhost:8080/swagger-ui.html
```

## 🔐 Security Notes

- Never commit `.env` or `application-dev.yml` files
- Use environment variables for all sensitive data
- Rotate AWS keys regularly
- Use strong JWT secrets (256-bit minimum)

## 🧪 Testing
```bash
# Run all tests
mvn test

# Run with coverage
mvn test jacoco:report
```

## 📦 Project Structure
```
src/main/java/com/resumescreening/api/
├── config/          # Configuration classes
├── controller/      # REST Controllers
├── service/         # Business Logic
├── repository/      # JPA Repositories
├── model/           # Entities & DTOs
├── security/        # Security components
├── exception/       # Custom Exceptions
└── util/            # Utility classes
```

## 🤝 Contributing

This is a personal learning project. Feedback and suggestions are welcome!

## 👤 Author

**Akshat Barve**
- Email: barveakshat091@gmail.com
- [LinkedIn](https://www.linkedin.com/in/akshatbarve/)
- [GitHub](https://github.com/barveakshat)

---

⭐ If you find this project helpful, please consider giving it a star!