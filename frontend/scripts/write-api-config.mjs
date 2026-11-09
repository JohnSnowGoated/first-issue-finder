import { writeFile } from 'node:fs/promises';

const apiUrl = (process.env.API_BASE_URL ?? '').replace(/\/$/, '');
if (apiUrl && !/^https:\/\//i.test(apiUrl)) {
  throw new Error('API_BASE_URL must be an https:// URL in production.');
}
await writeFile(new URL('../public/api-config.js', import.meta.url),
  `globalThis.FIRST_ISSUE_API_URL = ${JSON.stringify(apiUrl)};\n`, 'utf8');
