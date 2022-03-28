# DSFR prefixer

## Disclaimer

This script is meant to be a TEMPORARY SOLUTION in order to load dsfr component alongside bootstrap ones.
It is not guaranteed to be 100% effective, but has been tested in the scope of the mission, and it is fulfilling our needs in the meantime of a better solution.

## Presentation

This script is used to prefix every dsfr rules with the selector string: `#xwikimaincontainer #mainContentArea .use-dsfr`.
This has two advantages:
- scoping: no dsfr styles are going to mess with existing styles outside of an HTML element explicitly using it
- higher rule precedence: the dsfr rule gain more precedence, allowing to take over existing bootstrap rules

The prefixer will also take care of `:root`, `html` and `body` rules, to some extent:
```css
:root[data-theme="dark"] .my-class{
  /* ... */
}

/* will be transformed into: */

:root[data-theme="dark"] #xwikimaincontainer #mainContentArea .use-dsfr .my-class {
  /* ... */
}
```

However, for more complex rules, it might not work as intended:

```css
body > .direct-child {
  /* ... */
}

/* will be transformed into: */

body #xwikimaincontainer #mainContentArea .use-dsfr > .direct-child {
  /* ... */
}

/* instead of: */

body > .direct-child #xwikimaincontainer #mainContentArea .use-dsfr {
  /* ... */
}
```

However there are no such rules in the dsfr css files, so for the scope of this project this should not be a problem.

## Deployment

The prefixing of dsfr styles is a manual step in the deployment of the project, as it is not meant to be a final solution.

For this, first download this directory onto your machine.
You need to have node.js installed, the latest LTS version should work.

Inside the folder, fetch npm dependencies using:
```sh
$ npm install
```

Then simply execute `index.js` to generate the `dist/` folder of the dsfr, but with scoped styles:
```sh
$ node index.js
```

The generated folder should be named `dsfr-scoped/`, and you can now copy it (without modification) inside the `web/src/main/webapp/uicomponents/` folder of the project.

Note:
The dsfr version currently used is 1.3.1. The version has been pinned inside `package.json`, to prevent unwanted breakage. You can change the version there if you want, at your own risk.


## Usage

To use dsfr styles in the instance, simply add the `.use-dsfr` class to the HTML element you want to contain those styles:
```html
<div class="use-dsfr">
  Use DSFR here
</div>
```

This will only work for HTML element that are inside page content, more specifically inside the `#mainContentArea` HTML element.

