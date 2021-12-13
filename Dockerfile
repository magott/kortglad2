FROM openjdk:17-alpine
EXPOSE 8080
WORKDIR /app
RUN apk add --no-cache bash
COPY public/ ./public
COPY target/universal/stage ./
ENTRYPOINT bin/kortglad2