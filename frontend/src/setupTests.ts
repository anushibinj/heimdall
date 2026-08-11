import '@testing-library/jest-dom';
// @ts-expect-error missing node types in tsconfig
import { TextEncoder, TextDecoder } from 'util';
Object.assign(globalThis, { TextDecoder, TextEncoder });

class MockEventSource {
  onmessage: any = null;
  onerror: any = null;
  addEventListener = jest.fn();
  close = jest.fn();
  constructor(_url: string) {}
}

Object.assign(globalThis, { EventSource: MockEventSource });
