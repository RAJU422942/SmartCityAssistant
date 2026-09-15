import { AiResponseData } from '../models/aiModels';

export interface AiProvider {
  generateResponse(message: string): Promise<AiResponseData>;
}

export class LocalAiProvider implements AiProvider {
  async generateResponse(message: string): Promise<AiResponseData> {
    const trimmed = message.trim().toLowerCase();

    if (trimmed === 'hello' || trimmed === 'hi') {
      return {
        reply: "Hello! I am your Smart City Assistant.",
        intent: "GENERAL",
        action: null,
        parameters: {}
      };
    } else {
      return {
        reply: "Your Smart City AI Assistant is being connected to city services.",
        intent: "UNKNOWN",
        action: null,
        parameters: {}
      };
    }
  }
}

export class AiService {
  private provider: AiProvider;

  constructor(provider: AiProvider = new LocalAiProvider()) {
    this.provider = provider;
  }

  async generateChatResponse(message: string): Promise<AiResponseData> {
    if (!message || typeof message !== 'string' || message.trim() === '') {
      const error: any = new Error('Message is required.');
      error.statusCode = 400;
      error.errorCode = 'INVALID_MESSAGE';
      throw error;
    }

    if (message.length > 2000) {
      const error: any = new Error('Message exceeds maximum length of 2000 characters.');
      error.statusCode = 400;
      error.errorCode = 'INVALID_MESSAGE';
      throw error;
    }

    try {
      return await this.provider.generateResponse(message);
    } catch (err: any) {
      const error: any = new Error('AI service is temporarily unavailable.');
      error.statusCode = err.statusCode || 500;
      error.errorCode = err.errorCode || 'AI_SERVICE_ERROR';
      throw error;
    }
  }
}

export const aiService = new AiService();
