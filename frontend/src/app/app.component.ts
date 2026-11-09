import { HttpErrorResponse } from '@angular/common/http';
import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { Issue, IssueLabel, IssueSearchResponse } from './issue.model';
import { IssueService } from './issue.service';

@Component({
  selector: 'app-root',
  standalone: true,
  templateUrl: './app.component.html'
})
export class AppComponent implements OnInit {
  private readonly issueService = inject(IssueService);
  readonly languages = signal<string[]>(['Java', 'JavaScript', 'TypeScript', 'Python', 'C++', 'Go', 'Rust', 'Kotlin']);
  readonly selectedLanguage = signal('Java');
  readonly selectedLabels = signal<IssueLabel[]>(['good first issue']);
  readonly page = signal(1);
  readonly result = signal<IssueSearchResponse | null>(null);
  readonly loading = signal(false);
  readonly errorMessage = signal('');
  readonly savedView = signal(false);
  readonly savedIssues = signal<Issue[]>(this.loadSaved());
  readonly savedCount = computed(() => this.savedIssues().length);
  readonly visibleIssues = computed(() => this.savedView() ? this.savedIssues() : (this.result()?.items ?? []));
  readonly totalPages = computed(() => Math.max(1, Math.ceil((this.result()?.totalCount ?? 0) / (this.result()?.perPage ?? 20))));
  readonly pageNumbers = computed(() => {
    const last = this.totalPages();
    const current = this.page();
    return Array.from({ length: Math.min(5, last) }, (_, index) => {
      const start = Math.max(1, Math.min(current - 2, last - 4));
      return start + index;
    }).filter(number => number <= last);
  });

  ngOnInit(): void {
    this.issueService.languages().subscribe({
      next: available => { if (available.length) this.languages.set(available); },
      error: () => { /* The preset options keep the filters usable if the API is temporarily unavailable. */ }
    });
    this.search();
  }

  search(page = 1): void {
    this.savedView.set(false);
    this.page.set(page);
    this.loading.set(true);
    this.errorMessage.set('');
    this.issueService.search(this.selectedLanguage(), this.selectedLabels(), page).subscribe({
      next: response => { this.result.set(response); this.loading.set(false); },
      error: (error: unknown) => {
        this.loading.set(false);
        this.errorMessage.set(this.describeError(error));
      }
    });
  }

  chooseLanguage(value: string): void {
    this.selectedLanguage.set(value);
    this.search();
  }

  chooseLabel(value: string): void {
    const label = value as IssueLabel;
    const current = this.selectedLabels();
    if (current.length === 1 && current[0] === label) return;
    this.selectedLabels.set(current.includes(label) ? current.filter(item => item !== label) : [...current, label]);
    this.search();
  }

  setSavedView(enabled: boolean): void {
    this.savedView.set(enabled);
    if (!enabled && !this.result()) this.search();
  }

  isSaved(issue: Issue): boolean {
    return this.savedIssues().some(saved => saved.id === issue.id);
  }

  toggleSaved(issue: Issue): void {
    const next = this.isSaved(issue)
      ? this.savedIssues().filter(saved => saved.id !== issue.id)
      : [issue, ...this.savedIssues()];
    this.savedIssues.set(next);
    try {
      localStorage.setItem('first-issue-finder:saved', JSON.stringify(next));
    } catch {
      this.errorMessage.set('Bookmarks changed for this session, but could not be saved in this browser.');
    }
  }

  relativeDate(value: string): string {
    const days = Math.max(0, Math.floor((Date.now() - new Date(value).getTime()) / 86_400_000));
    if (days === 0) return 'today';
    if (days === 1) return '1 day ago';
    if (days < 30) return `${days} days ago`;
    const months = Math.floor(days / 30);
    return months === 1 ? '1 month ago' : `${months} months ago`;
  }

  trackIssue(_index: number, issue: Issue): number {
    return issue.id;
  }

  private loadSaved(): Issue[] {
    try {
      const stored = localStorage.getItem('first-issue-finder:saved');
      return stored ? JSON.parse(stored) as Issue[] : [];
    } catch {
      return [];
    }
  }

  private describeError(error: unknown): string {
    if (error instanceof HttpErrorResponse && (error.status === 429 || error.status === 403)) {
      return 'GitHub is limiting requests right now. Wait a little and try again.';
    }
    if (error instanceof HttpErrorResponse && error.status === 0) {
      return 'Could not reach the API. Check that the backend is running, then retry.';
    }
    return 'Something went wrong while searching GitHub. Please try again.';
  }
}
