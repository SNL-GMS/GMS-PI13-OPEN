/**
 * Replaces favicon with dark mode icon if found.
 */
export function replaceFavIcon(logo: any): void {
  const favLink: HTMLLinkElement =
    document.querySelector("link[rel*='icon']") || document.createElement('link');

  favLink.type = 'image/x-icon';
  favLink.rel = 'shortcut icon';
  favLink.href = logo ? logo : 'gms-logo.ico';

  document.getElementsByTagName('head')[0].appendChild(favLink);
}

/**
 * Return true if the browser knows to prefer dark mode, like Mojave's dark mode and some browser themes
 */
export function isDarkMode(): boolean {
  return window.matchMedia('(prefers-color-scheme: dark)').matches;
}
