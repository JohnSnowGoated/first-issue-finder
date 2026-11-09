# First Issue Finder

Find beginner-friendly open-source issues by programming language and GitHub label. This portfolio project uses an Angular frontend, a Spring Boot API, GitHub's public issue search API, and browser storage for saved issues.

## Features

- Search open issues by programming language and `good first issue` / `help wanted` labels.
- Exclude pull requests, paginate results, and open the issue on GitHub.
- Save issues in the browser; bookmarks remain after a refresh without an account.
- Show loading, empty, API error, and rate-limit states.
- Cache identical searches briefly in the API to reduce calls to GitHub.

## Requirements

- Java 17 or newer
- Maven 3.9+
- Node.js 20.19+ and npm

## Run locally

1. Start the API:

   ```powershell
   cd backend
   mvn spring-boot:run
   ```

2. In a second terminal, start Angular:

   ```powershell
   cd frontend
   npm ci
   npm start
   ```

3. Open <http://localhost:4200>. The Angular development server proxies `/api` requests to Spring Boot on port 8080.

Run the checks with `cd backend && mvn test` and `cd frontend && npm test` (from separate terminals or one at a time).

An optional GitHub token can be set as `GITHUB_TOKEN` for a higher API limit. Never put it in Angular or commit it. For local PowerShell:

```powershell
$env:GITHUB_TOKEN = "your-token"
```

## API

`GET /api/issues?language=Java&label=good%20first%20issue&page=1`

Returns `items`, `totalCount`, `page`, and `perPage`. Supported labels are `good first issue` and `help wanted`; supported languages are returned by `GET /api/languages`. `GET /api/health` is a lightweight health endpoint.

## Resume bullet

Built a full-stack open-source issue finder with Angular and Spring Boot, integrating GitHub issue search with cached API results, rate-limit handling, pagination, and browser-persisted bookmarks.

## Deploy on Render

The repository is split into `frontend/` and `backend/`, so deploy each as a separate Render service:

### API web service

1. Create a Render Web Service from this repository and set its root directory to `backend`.
2. Choose the Docker runtime so Render builds `backend/Dockerfile`.
3. Set `APP_CORS_ALLOWED_ORIGIN` to the final Angular site origin (for example, `https://first-issue-finder.onrender.com`). Optionally set `GITHUB_TOKEN` as a secret environment variable.
4. Set the health check path to `/api/health`.

### Angular static site

1. Create a Render Static Site from this repository with root directory `frontend`.
2. Use build command `npm ci && npm run build` and publish directory `dist/first-issue-finder/browser`.
3. Set the Render build environment variable `API_BASE_URL` to the API service URL, including `https://` and no trailing slash. The build writes it to a runtime config file served with the Angular app.

Each service redeploys when its linked branch changes. Free Render web services can sleep after inactivity, so an initial API request after a quiet period may be slow. Update the frontend API URL after both services exist and confirm the browser can search, bookmark, and display API errors.

## Project structure

```text
frontend/  Angular UI and local bookmarks
backend/   Spring Boot API, GitHub client, and Dockerfile
```

