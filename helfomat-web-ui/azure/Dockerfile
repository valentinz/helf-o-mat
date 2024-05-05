FROM node:12.16.3-slim as builder

WORKDIR /usr/src/app

COPY package.json ./
COPY npm-shrinkwrap.json ./
RUN npm install

COPY . .
RUN npm run-script build-prod




FROM nginx:1.17.10-alpine

RUN rm -rf /usr/share/nginx/html
COPY --from=builder /usr/src/app/dist /usr/share/nginx/html
COPY ./azure/nginx.conf /etc/nginx/conf.d/default.conf