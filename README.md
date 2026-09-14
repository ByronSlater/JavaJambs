# JavaJambs

JavaJambs is a Spring Boot application with a Thymeleaf frontend, Tailwind CSS, DaisyUI, PostgreSQL, Flyway, and JPA.

## Prerequisites

Install the following locally:

- Java 21
- Node.js and npm
- PostgreSQL
- GNU Make

The Maven wrapper is included with the repo so you don't need a
dedicated Maven installation

## Install

Clone the repo and enter the directory

```bash
git clone https://github.com/ByronSlater/JavaJambs.git
cd JavaJambs
```

Install frontend tooling with

```bash
npm install
```

Create the dev db, it'll be owned by your current user

```bash
createdb cher_development
```
or
```bash
make db-up
```

Copy the environment example and update the username, password, and database name for your local PostgreSQL installation:

```bash
cp .env.example .env
```

For a local PostgreSQL role named `your_user` with no password, the relevant values are:

```env
SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/cher_development
SPRING_DATASOURCE_USERNAME=your_user
SPRING_DATASOURCE_PASSWORD=
SPRING_FLYWAY_URL=jdbc:postgresql://localhost:5432/cher_development
SPRING_FLYWAY_USER=your_user
SPRING_FLYWAY_PASSWORD=
```

`.env` is ignored by git and should not be committed.

## Run Locally

Start Spring Boot and the Tailwind CSS watcher together:

```bash
make run
```

The application is available at [http://localhost:8080](http://localhost:8080). Press `Ctrl-C` to stop both processes.

The Tailwind watcher writes the generated stylesheet to `target/classes/static/css/output.css`, which is the location served by the running Spring Boot application.

To build the CSS once without starting the application:

```bash
npm run build:css
```

## Tests

Create a separate test database:

```bash
createdb cher_test
cp .env.test.example .env.test
```

Update `.env.test` with your local PostgreSQL role and password, then run:

```bash
make test
```

The test command loads `.env.test` using `-Ddotenv.file=.env.test`. Normal application runs load `.env` by default.

## Useful Commands

```bash
make help       # Show all Make targets
```

## Docker Compose

Docker is partially setup, the compose should run a postgres and elastisearch
container up but they're not linked in currently

```bash
docker compose up -d
```

Stop the services with:

```bash
docker compose down
```
