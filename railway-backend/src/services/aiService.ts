import { GoogleGenAI } from '@google/genai';
import { AiResponseData } from '../models/aiModels';

export interface AiProvider {
  generateResponse(message: string): Promise<AiResponseData>;
}

const ALLOWED_INTENTS = new Set([
  'GENERAL',
  'EMERGENCY',
  'AQI',
  'NEARBY',
  'TRANSPORT',
  'RAILWAY',
  'GOVERNMENT',
  'SCHEME',
  'COMPLAINT',
  'MY_COMPLAINTS',
  'CITY_ALERT',
  'UNKNOWN'
]);

const ALLOWED_ACTIONS = new Set([
  'OPEN_EMERGENCY',
  'OPEN_AQI',
  'OPEN_NEARBY',
  'OPEN_TRANSPORT',
  'OPEN_RAILWAY',
  'OPEN_GOVERNMENT',
  'OPEN_SCHEMES',
  'OPEN_REPORT_PROBLEM',
  'OPEN_MY_COMPLAINTS',
  'OPEN_CITY_ALERTS'
]);

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
    } else if (trimmed.includes('hospital')) {
      return {
        reply: "I can help you find a nearby hospital using our services.",
        intent: "NEARBY",
        action: "OPEN_NEARBY",
        parameters: { category: "HOSPITAL" }
      };
    } else if (trimmed.includes('aqi')) {
      return {
        reply: "I can check the current air quality index for you.",
        intent: "AQI",
        action: "OPEN_AQI",
        parameters: {}
      };
    } else if (trimmed.includes('student') || trimmed.includes('scheme')) {
      return {
        reply: "I can open government schemes for you.",
        intent: "SCHEME",
        action: "OPEN_SCHEMES",
        parameters: { category: "STUDENT" }
      };
    } else if (trimmed.includes('danger') || trimmed.includes('emergency') || trimmed.includes('police')) {
      return {
        reply: "If this is an immediate emergency, please use the Emergency section or call 112.",
        intent: "EMERGENCY",
        action: "OPEN_EMERGENCY",
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

export class GeminiAiProvider implements AiProvider {
  private ai: GoogleGenAI;
  private modelName: string;

  constructor() {
    const apiKey = process.env.GEMINI_API_KEY;
    this.ai = new GoogleGenAI(apiKey ? { apiKey } : {});
    this.modelName = process.env.GEMINI_MODEL || 'gemini-2.5-flash';
  }

  async generateResponse(message: string): Promise<AiResponseData> {
    const systemInstruction = `You are the AI Assistant inside "Smart City Assistant", a smart city mobile application for Indian cities.
You help citizens with city services, transport, government schemes, AQI, and emergencies.
Rules:
1. Be concise, helpful, and polite.
2. NEVER invent live data, AQI values, train numbers, hospital names, government schemes, or emergency contact numbers.
3. Distinguish general knowledge from live app data.
4. Return your response STRICTLY as a JSON object with the following fields:
   - "reply": conversational response to the user.
   - "intent": must be one of [GENERAL, EMERGENCY, AQI, NEARBY, TRANSPORT, RAILWAY, GOVERNMENT, SCHEME, COMPLAINT, MY_COMPLAINTS, CITY_ALERT, UNKNOWN].
   - "action": must be one of [OPEN_EMERGENCY, OPEN_AQI, OPEN_NEARBY, OPEN_TRANSPORT, OPEN_RAILWAY, OPEN_GOVERNMENT, OPEN_SCHEMES, OPEN_REPORT_PROBLEM, OPEN_MY_COMPLAINTS, OPEN_CITY_ALERTS] or null.
   - "parameters": object containing optional parameters (e.g., {"category": "HOSPITAL"} or {"category": "STUDENT"}).

Examples:
- "Find a hospital near me" -> {"reply": "I can help you find a nearby hospital using our services.", "intent": "NEARBY", "action": "OPEN_NEARBY", "parameters": {"category": "HOSPITAL"}}
- "Check AQI" -> {"reply": "I can check the current air quality index for you.", "intent": "AQI", "action": "OPEN_AQI", "parameters": {}}
- "I am in danger" -> {"reply": "If this is an immediate emergency, please use the Emergency section or call 112.", "intent": "EMERGENCY", "action": "OPEN_EMERGENCY", "parameters": {}}
- "Hello" -> {"reply": "Hello! How can I assist you with city services today?", "intent": "GENERAL", "action": null, "parameters": {}}`;

    try {
      const response = await this.ai.models.generateContent({
        model: this.modelName,
        contents: message,
        config: {
          systemInstruction,
          responseMimeType: 'application/json',
          temperature: 0.3
        }
      });

      const text = response.text;
      if (!text) {
        throw new Error('Empty response from Gemini AI');
      }

      let cleanedText = text.trim();
      if (cleanedText.startsWith('```json')) {
        cleanedText = cleanedText.replace(/^```json/, '').replace(/```$/, '').trim();
      } else if (cleanedText.startsWith('```')) {
        cleanedText = cleanedText.replace(/^```/, '').replace(/```$/, '').trim();
      }

      const parsed = JSON.parse(cleanedText);

      const reply = typeof parsed.reply === 'string' ? parsed.reply : 'I am here to help you with Smart City services.';

      let intent = typeof parsed.intent === 'string' ? parsed.intent.toUpperCase() : 'UNKNOWN';
      if (!ALLOWED_INTENTS.has(intent)) {
        intent = 'UNKNOWN';
      }

      let action: string | null = null;
      if (parsed.action && typeof parsed.action === 'string') {
        const upperAction = parsed.action.toUpperCase();
        if (ALLOWED_ACTIONS.has(upperAction)) {
          action = upperAction;
        }
      }

      const parameters = (parsed.parameters && typeof parsed.parameters === 'object' && !Array.isArray(parsed.parameters))
        ? parsed.parameters
        : {};

      return {
        reply,
        intent,
        action,
        parameters
      };

    } catch (err: any) {
      console.error('[AiService]: Gemini AI error code:', err.code || err.message || 'UNKNOWN');
      const error: any = new Error('AI service is temporarily unavailable.');
      error.statusCode = 503;
      error.errorCode = 'AI_SERVICE_ERROR';
      throw error;
    }
  }
}

export class AiService {
  private provider: AiProvider;

  constructor() {
    const apiKey = process.env.GEMINI_API_KEY;
    if (apiKey && apiKey.trim() !== '') {
      this.provider = new GeminiAiProvider();
      console.log('[AiService]: Initialized with GeminiAiProvider.');
    } else {
      console.warn('[AiService]: GEMINI_API_KEY not configured. Falling back to LocalAiProvider.');
      this.provider = new LocalAiProvider();
    }
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
