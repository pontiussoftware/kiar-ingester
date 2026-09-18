import {computed, inject, Injectable, signal} from "@angular/core";
import {DOCUMENT} from "@angular/common";
import {TranslateService} from "@ngx-translate/core";
import {firstValueFrom} from "rxjs";

/** The languages supported by the UI. */
export type Language = 'en' | 'de';

/** All supported {@link Language}s in display order. */
export const LANGUAGES: ReadonlyArray<Language> = ['en', 'de'];

/** The key under which the selected language is persisted in local storage. */
const STORAGE_KEY = 'kiar.language';

/**
 * Manages the UI language: detection at start-up, switching at runtime and persistence in the browser.
 *
 * The current language is exposed as a signal so that templates (e.g. date formatting) can react to changes.
 */
@Injectable({
  providedIn: 'root',
})
export class LanguageService {
  /** The {@link TranslateService} that loads and applies the translation files. */
  private translate = inject(TranslateService);

  /** The {@link Document} whose `lang` attribute is kept in sync with the selected language. */
  private document = inject(DOCUMENT);

  /** The currently selected {@link Language}. */
  private readonly _language = signal<Language>(this.detectInitialLanguage());

  /** A read-only signal of the currently selected {@link Language}. */
  public readonly language = this._language.asReadonly();

  /** A signal of the locale identifier used for date and number formatting. */
  public readonly locale = computed(() => this._language() === 'de' ? 'de-CH' : 'en-GB');

  /**
   * Loads the translations for the detected language. Invoked once during application initialisation,
   * before the first component is rendered.
   */
  public initialize(): Promise<unknown> {
    this.translate.addLangs([...LANGUAGES]);
    return firstValueFrom(this.apply(this._language()));
  }

  /**
   * Switches the UI to the given {@link Language} and persists the choice for this browser.
   *
   * @param language The {@link Language} to switch to.
   */
  public use(language: Language) {
    if (language === this._language()) return;
    try {
      localStorage.setItem(STORAGE_KEY, language);
    } catch (e) {
      /* Storage unavailable (e.g. private mode); the choice simply won't persist. */
    }
    this.apply(language).subscribe();
  }

  /**
   * Applies the given {@link Language} to the translation service, the signal and the document.
   *
   * @param language The {@link Language} to apply.
   */
  private apply(language: Language) {
    this._language.set(language);
    this.document.documentElement.lang = language;
    return this.translate.use(language);
  }

  /**
   * Determines the initial {@link Language}: a stored choice wins, then the browser language, then English.
   */
  private detectInitialLanguage(): Language {
    try {
      const stored = localStorage.getItem(STORAGE_KEY);
      if (stored && LANGUAGES.includes(stored as Language)) {
        return stored as Language;
      }
    } catch (e) {
      /* Storage unavailable; fall through to browser detection. */
    }
    return navigator.language?.toLowerCase().startsWith('de') ? 'de' : 'en';
  }
}
