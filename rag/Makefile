.PHONY: setup lint test run clean

setup:
	mvn -B dependency:go-offline

lint:
	mvn -B -DskipTests verify

test:
	mvn test

run:
	mvn spring-boot:run

clean:
	mvn clean
