# Local development

```bash
# Get source
git clone git@github.com:apigovau/service-catalogue-repository.git
cd service-catalogue-repository/

# Build it
gradle build

# Create a PostgreSQL instance
docker run --name api-gov-au-service-catalogue-db -p 5434:5432 -e POSTGRES_PASSWORD=mysecretpassword -d postgres:9.6

# Run it
SPRING_DATASOURCE_URL="jdbc:postgresql://localhost:5434/postgres?user=postgres&password=mysecretpassword" \
    java -Xmx200m -Xss512k -jar build/libs/service-catalogue-repository-1.0.jar

# In another terminal...

# Create new service
SERVICE_ID="$(curl http://localhost:5001/new?authorization=ignored | jq -r .id)"

# Get service
curl "http://localhost:5001/repository/service/${SERVICE_ID}" | jq .

# List all services
curl http://localhost:5001/repository/index | jq .
```
