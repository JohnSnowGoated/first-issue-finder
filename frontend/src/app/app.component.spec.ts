import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting, HttpTestingController } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { AppComponent } from './app.component';
import { Issue } from './issue.model';

describe('AppComponent', () => {
  let http: HttpTestingController;
  const stored = new Map<string, string>();

  beforeEach(() => {
    stored.clear();
    vi.stubGlobal('localStorage', {
      getItem: (key: string) => stored.get(key) ?? null,
      setItem: (key: string, value: string) => stored.set(key, value)
    });
    TestBed.configureTestingModule({
      imports: [AppComponent],
      providers: [provideHttpClient(), provideHttpClientTesting()]
    });
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    http.verify();
    TestBed.resetTestingModule();
    vi.unstubAllGlobals();
  });

  it('loads the initial issue results', () => {
    const fixture = TestBed.createComponent(AppComponent);
    fixture.detectChanges();

    http.expectOne('/api/languages').flush(['Java', 'TypeScript']);
    http.expectOne(request => request.url === '/api/issues').flush({
      items: [issue], totalCount: 1, page: 1, perPage: 20
    });

    expect(fixture.componentInstance.visibleIssues()).toEqual([issue]);
    expect(fixture.componentInstance.loading()).toBe(false);
  });

  it('uses both selected labels as filters', () => {
    const fixture = TestBed.createComponent(AppComponent);
    fixture.detectChanges();
    http.expectOne('/api/languages').flush(['Java']);
    http.expectOne(request => request.url === '/api/issues').flush({
      items: [], totalCount: 0, page: 1, perPage: 20
    });

    fixture.componentInstance.chooseLabel('help wanted');
    const request = http.expectOne(candidate => candidate.url === '/api/issues');

    expect(request.request.params.getAll('label')).toEqual(['good first issue', 'help wanted']);
    request.flush({ items: [], totalCount: 0, page: 1, perPage: 20 });
  });

  it('saves and removes issues in browser storage', () => {
    const fixture = TestBed.createComponent(AppComponent);
    fixture.detectChanges();
    http.expectOne('/api/languages').flush(['Java']);
    http.expectOne(request => request.url === '/api/issues').flush({
      items: [], totalCount: 0, page: 1, perPage: 20
    });

    fixture.componentInstance.toggleSaved(issue);
    expect(JSON.parse(stored.get('first-issue-finder:saved') ?? '[]')).toEqual([issue]);
    expect(fixture.componentInstance.isSaved(issue)).toBe(true);

    fixture.componentInstance.toggleSaved(issue);
    expect(JSON.parse(stored.get('first-issue-finder:saved') ?? '[]')).toEqual([]);
  });
});

const issue: Issue = {
  id: 7,
  number: 42,
  title: 'Add a keyboard shortcut',
  url: 'https://github.com/acme/project/issues/42',
  repository: 'acme/project',
  labels: ['good first issue'],
  createdAt: '2026-01-01T00:00:00Z',
  updatedAt: '2026-02-01T00:00:00Z',
  comments: 3
};
