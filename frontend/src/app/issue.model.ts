export interface Issue {
  id: number;
  number: number;
  title: string;
  url: string;
  repository: string;
  labels: string[];
  createdAt: string;
  updatedAt: string;
  comments: number;
}

export interface IssueSearchResponse {
  items: Issue[];
  totalCount: number;
  page: number;
  perPage: number;
}

export type IssueLabel = 'good first issue' | 'help wanted';
