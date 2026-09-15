export interface AiMessageRequest {
  message: string;
}

export interface AiActionCardData {
  title: string;
  description: string;
  actionText?: string;
  intent?: string;
  action?: string;
  parameters?: Record<string, string>;
}

export interface AiResponseData {
  reply: string;
  intent: string;
  action: string | null;
  parameters: Record<string, any>;
  actionCard?: AiActionCardData;
}

export interface AiApiResponse {
  success: boolean;
  data?: AiResponseData;
  error?: {
    code: string;
    message: string;
  };
}
