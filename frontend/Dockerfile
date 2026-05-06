# Single stage — Node builds AND serves the app. No Nginx needed.
FROM node:20-alpine

WORKDIR /app

# package-lock.json IS present in your project so npm ci works correctly
COPY package.json package-lock.json ./
RUN npm install --silent

# Install 'serve' to host the production build (replaces Nginx)
RUN npm install -g serve --silent

# Copy all source including public/index.html (fixes "Could not find index.html")
COPY . .

# REACT_APP_API_URL injected at build time from docker-compose args
ARG REACT_APP_API_URL=http://localhost:8080/api
ENV REACT_APP_API_URL=$REACT_APP_API_URL

# CI=false prevents ESLint warnings from failing the build
ENV CI=false

RUN npm run build

EXPOSE 3000

# Serve the React build on port 3000 — no Nginx, no proxy config needed
CMD ["serve", "-s", "build", "-l", "3000"]
