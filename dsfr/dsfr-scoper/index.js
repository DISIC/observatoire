
import fs from 'fs/promises';
import path from 'path';
import postcss from 'postcss';
import prefixer from 'postcss-prefix-selector';

// Get source files from dsfr dependency
const sourceDir = './node_modules/@gouvfr/dsfr/dist';

// Remove target dir if it exists
const targetDir = './dsfr-scoped';
try {
  await fs.lstat(targetDir);
    console.log(`Removing existing target dir "${targetDir}"`);
    await fs.rm(targetDir, { recursive: true, force: true });
} catch {};

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
};

// For given input file, add prefix for every rules
// Return css as string
const transformFile = async (sourceFile, targetFile) => {
  console.log(`Transforming: ${targetFile}`);
  const css = await fs.readFile(sourceFile);
  const result = await postcss()
    .use(prefixer({ prefix, transform }))
    .process(css, { from: sourceFile, to: targetFile});
  return result.css;
};


// Recursively copy sourceDir to targetDir,
// modifying every css file encountered
const copyDirRecursive = async (sourceDir, targetDir) => {
  // Create new target dir
  await fs.mkdir(targetDir);

  // Copy
  const files = await fs.readdir(sourceDir);
  files.forEach(async filename => {
    const sourceFile = path.join(sourceDir, filename);
    const sourceFileStat = await fs.lstat(sourceFile);
    if (sourceFileStat.isDirectory()) {
      const childTargetDir = path.join(targetDir, filename);
      copyDirRecursive(sourceFile, childTargetDir);
    } else {
      copyFile(sourceFile, targetDir);
    }
  });
};

const copyFile = async (sourceFile, targetDir) => {
  const targetFile = path.join(targetDir, path.basename(sourceFile));
  if (sourceFile.endsWith('.css')) {
    const css = await transformFile(sourceFile, targetFile);
    fs.writeFile(targetFile, css);
  } else {
    const css = await fs.readFile(sourceFile);
    fs.writeFile(targetFile, css);
  }
}


// Actually execute progam
copyDirRecursive(sourceDir, targetDir);


