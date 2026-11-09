import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { IssueLabel, IssueSearchResponse } from './issue.model';

@Injectable({ providedIn: 'root' })
export class IssueService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = this.readApiBaseUrl();

  search(language: string, labels: IssueLabel[], page: number): Observable<IssueSearchResponse> {
    let params = new HttpParams().set('page', page);
    if (language) params = params.set('language', language);
    labels.forEach(label => params = params.append('label', label));
    return this.http.get<IssueSearchResponse>(`${this.baseUrl}/api/issues`, { params });
  }

  languages(): Observable<string[]> {
    return this.http.get<string[]>(`${this.baseUrl}/api/languages`);
  }

  private readApiBaseUrl(): string {
    const configured = (globalThis as typeof globalThis & { FIRST_ISSUE_API_URL?: string }).FIRST_ISSUE_API_URL;
    return (configured ?? '').replace(/\/$/, '');
  }
}
