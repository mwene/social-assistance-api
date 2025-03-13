# Getting Started
http://localhost:8080/swagger-ui.html
 - Login: POST /api/auth/login with {"username": "admin", "password": "password123"}.
 - Register: POST /api/auth/register with {"username": "newuser", "password": "pass123", "name": "New User", "email": "new@example.com", "phone": "1234567890"}.
 - Uploads: Use the JWT in Authorization: Bearer <token> for /api/uploads/applicants.

### Build 
./gradlew build  # If using Gradle
mvn clean package  # If using Maven

### Docker Image
docker build -t social-assistance:latest .

### Run with PostgreSQL
docker run -d --name social-app \
  -p 8080:8080 \
  -e SPRING_DATASOURCE_URL=jdbc:postgresql://host.docker.internal:5432/socialdb \
  -e SPRING_DATASOURCE_USERNAME=socialuser \
  -e SPRING_DATASOURCE_PASSWORD=socialpass \
  -e JWT_SECRET=your-secure-jwt-secret-here \
  social-assistance:latest
  
### Run with MySQL (swap DB settings)
docker run -d --name social-app \
  -p 8080:8080 \
  -e SPRING_DATASOURCE_URL=jdbc:mysql://host.docker.internal:3306/socialdb \
  -e SPRING_DATASOURCE_USERNAME=socialuser \
  -e SPRING_DATASOURCE_PASSWORD=socialpass \
  -e JWT_SECRET=your-secure-jwt-secret-here \
  social-assistance:latest
  
## AWS Deployment
### Prerequisites
 - AWS CLI: Installed and configured (aws configure).
 - EB CLI: Installed (pip install awsebcli).
### Configuration
 - Create Dockerrun.aws.json in the project root:
 {
  "AWSEBDockerrunVersion": "1",
  "Image": {
    "Name": "social-assistance:latest",
    "Update": "true"
  },
  "Ports": [
    {
      "ContainerPort": 8080,
      "HostPort": 80
    }
  ],
  "Logging": "/var/log"
}

### initialize beanstalk:
eb init -p docker social-assistance-aws --region us-east-1

### create environment:
eb create social-assistance-env \
  --envvars SPRING_DATASOURCE_URL=jdbc:postgresql://<aws-rds-endpoint>:5432/socialdb,SPRING_DATASOURCE_USERNAME=socialuser,SPRING_DATASOURCE_PASSWORD=socialpass,JWT_SECRET=your-secure-jwt-secret-here
   - Replace <aws-rds-endpoint> with your RDS PostgreSQL instance endpoint.

### Deploy:
eb deploy

### Verify: 
 - Access the URL provided by eb status (e.g., http://social-assistance-env.us-east-1.elasticbeanstalk.com).
 
## GCP Deployment (Google App Engine)
### Prerequisites:
 - Google Cloud SDK: Installed and configured (gcloud init).

### Configuration:
Create app.yaml in the project root:
runtime: custom
env: flex
manual_scaling:
  instances: 1
resources:
  cpu: 1
  memory_gb: 0.5
  disk_size_gb: 10
env_variables:
  SPRING_DATASOURCE_URL: "jdbc:postgresql:///<your-gcp-sql-instance-connection-name>/socialdb?socketFactory=com.google.cloud.sql.postgres.SocketFactory"
  SPRING_DATASOURCE_USERNAME: "socialuser"
  SPRING_DATASOURCE_PASSWORD: "socialpass"
  JWT_SECRET: "your-secure-jwt-secret-here"

### deploy:
gcloud app deploy --image-url=gcr.io/<your-project-id>/social-assistance:latest

 - Push the Docker image to Google Container Registry first:
 docker tag social-assistance:latest gcr.io/<your-project-id>/social-assistance:latest
docker push gcr.io/<your-project-id>/social-assistance:latest

### Verify:
 - Access https://<your-project-id>.appspot.com.

# GitHub Actions for Automated Deployment
## Workflow File
 - Create .github/workflows/deploy.yml in your repository:
 name: Deploy to AWS or GCP

on:
  push:
    branches:
      - main
  workflow_dispatch:
    inputs:
      platform:
        description: 'Deployment platform (aws or gcp)'
        required: true
        default: 'aws'

jobs:
  build-and-deploy:
    runs-on: ubuntu-latest
    steps:
      - name: Checkout code
        uses: actions/checkout@v3

      - name: Set up JDK 17
        uses: actions/setup-java@v3
        with:
          java-version: '17'
          distribution: 'temurin'

      - name: Build with Maven
        run: mvn clean package -DskipTests

      - name: Build Docker image
        run: docker build -t social-assistance:latest .

      - name: Deploy to AWS
        if: github.event.inputs.platform == 'aws' || github.event_name == 'push'
        env:
          AWS_ACCESS_KEY_ID: ${{ secrets.AWS_ACCESS_KEY_ID }}
          AWS_SECRET_ACCESS_KEY: ${{ secrets.AWS_SECRET_ACCESS_KEY }}
          AWS_REGION: us-east-1
        run: |
          aws ecr get-login-password --region $AWS_REGION | docker login --username AWS --password-stdin <your-aws-account-id>.dkr.ecr.$AWS_REGION.amazonaws.com
          docker tag social-assistance:latest <your-aws-account-id>.dkr.ecr.$AWS_REGION.amazonaws.com/social-assistance:latest
          docker push <your-aws-account-id>.dkr.ecr.$AWS_REGION.amazonaws.com/social-assistance:latest
          eb setenv SPRING_DATASOURCE_URL=jdbc:postgresql://<aws-rds-endpoint>:5432/socialdb SPRING_DATASOURCE_USERNAME=socialuser SPRING_DATASOURCE_PASSWORD=socialpass JWT_SECRET=${{ secrets.JWT_SECRET }}
          eb deploy social-assistance-env

      - name: Deploy to GCP
        if: github.event.inputs.platform == 'gcp'
        env:
          GOOGLE_CREDENTIALS: ${{ secrets.GCP_SA_KEY }}
        run: |
          echo "$GOOGLE_CREDENTIALS" > gcp-key.json
          gcloud auth activate-service-account --key-file=gcp-key.json
          gcloud config set project <your-project-id>
          docker tag social-assistance:latest gcr.io/<your-project-id>/social-assistance:latest
          docker push gcr.io/<your-project-id>/social-assistance:latest
          gcloud app deploy app.yaml --quiet --image-url=gcr.io/<your-project-id>/social-assistance:latest

    env:
      EB_CLI_VERSION: 3.20.3
    steps:
      - name: Install EB CLI
        if: github.event.inputs.platform == 'aws' || github.event_name == 'push'
        run: pip install awsebcli==$EB_CLI_VERSION

## Secrets Setup
In your GitHub repository settings:

### AWS:
 - AWS_ACCESS_KEY_ID: Your AWS access key.
 - AWS_SECRET_ACCESS_KEY: Your AWS secret key.
 - JWT_SECRET: Your JWT secret.
### GCP:
 - GCP_SA_KEY: JSON key for your GCP service account (base64-encoded if needed).
 - JWT_SECRET: Same as above.
### Usage
 - Push to main: Deploys to AWS by default.
 - Manual Trigger:
   - Go to Actions > Deploy to AWS or GCP > Run workflow.
   - Select aws or gcp in the platform input.

## Verification
 - Local: curl http://localhost:8080/api/auth/login.
 - AWS: Check Elastic Beanstalk URL.
 - GCP: Check App Engine URL.
