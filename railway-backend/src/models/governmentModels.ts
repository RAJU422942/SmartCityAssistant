export interface GovScheme {
  id: string;
  title: string;
  description: string;
  category: string;
  eligibility: string;
  officialSource: string;
  officialUrl: string;
}

export interface WelfareBenefit {
  id: string;
  name: string;
  description: string;
  eligibility: string;
  requiredDocuments: string[];
  howToApply: string;
  officialSource: string;
}

export interface GovNotice {
  id: string;
  title: string;
  summary: string;
  date: string;
  authority: string;
  sourceUrl: string;
}

export interface GovHelpline {
  id: string;
  name: string;
  number: string;
  purpose: string;
  category: string;
}
