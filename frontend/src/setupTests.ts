import '@testing-library/jest-dom';
import { TextEncoder, TextDecoder } from 'util';
Object.assign(globalThis, { TextDecoder, TextEncoder });

class MockEventSource {
  onmessage: any = null;
  onerror: any = null;
  addEventListener = jest.fn();
  close = jest.fn();
  constructor(url: string) {}
}

Object.assign(globalThis, { EventSource: MockEventSource });
