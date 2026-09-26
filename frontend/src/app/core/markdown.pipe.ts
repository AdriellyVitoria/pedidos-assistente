import { Pipe, PipeTransform } from '@angular/core';
import { marked } from 'marked';

@Pipe({ name: 'markdown' })
export class MarkdownPipe implements PipeTransform {
  transform(texto: string): string {
    return marked.parse(texto, { async: false, gfm: true, breaks: true });
  }
}
