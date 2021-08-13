
const fs = require('fs');
const postcss = require('postcss')
const prefixer = require('postcss-prefix-selector')

// Get input files
const dir = './css';
const files = fs.readdirSync(dir);
const cssFiles = files.filter(file => file.endsWith('.css'));

// Create output dir if not exists
const outDir = './out';
fs.mkdirSync(outDir, { recursive: true });

// The prfix to add for every rules
const prefix = '#xwikimaincontainer #mainContentArea #avis-tab';

// Regex
const createRegex = selector => new RegExp(`^(.*?)(${selector}[^\\s]*)(.*?)$`);
const rootRegex = createRegex(":root");
const htmlRegex = createRegex("html");
const bodyRegex = createRegex("body");

const replaceSpecialRule = (selector, regex) => selector.replace(regex, `$1$2 ${prefix}$3`);

// Rule transform callback
// Here we need to prevent `:root` `html` or `body` rules to be prefixed
// and instead insert our prefix right after this selector
// Please see readme for more information
const transform = function (prefix, selector, prefixedSelector) {
  // Search for specific rules, and adapt the prefixing
  // Search first for `body` rules, then `html`, then `:root`
  // So that whenever there is a combinaison of these keywords on a same rule,
  // we append the prefix at the right place
  if (bodyRegex.test(selector)) {
    return replaceSpecialRule(selector, bodyRegex);
  } else if (htmlRegex.test(selector)) {
    return replaceSpecialRule(selector, htmlRegex);
  } if (rootRegex.test(selector)) {
    return replaceSpecialRule(selector, rootRegex);
  } else {
    return prefixedSelector;
  }
}

// For every input files, add prefix for every rules
for (const file of cssFiles) {
  const srcFile = `./${dir}/${file}`;
  const outFile = `./${outDir}/${file}`;
  console.log(srcFile, ' --> ', outFile);
  fs.readFile(srcFile, (err, css) => {
    postcss()
      .use(prefixer({
        prefix,
        transform,
      }))
      .process(css, { from: srcFile, to: outFile})
      .then(result => {
        fs.writeFile(outFile, result.css, () => true);
      });
  })
}
