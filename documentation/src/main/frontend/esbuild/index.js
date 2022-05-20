const dev = process.env.NODE_ENV === 'dev';
const esbuild = require('esbuild');
const { sassPlugin, postcssModules } = require('esbuild-sass-plugin');
const path = require('path');
const fs = require('fs');

const readProjectVersion = () => {
    const pom = '../../../pom.xml';
    const content = fs.readFileSync(pom).toString('UTF-8');
    const from = content.indexOf('<version>') + '<version>'.length;
    const end = content.indexOf('</version>', from);
    return content.substring(from, end).trim();
};

const projectVersion = process.env.PROJECT_VERSION || readProjectVersion();
const outDir = `../../../target/documentation-${projectVersion}/`;
const indexHtmlVersion = projectVersion.endsWith('-SNAPSHOT') ?
    (process.env.BUILD_DATE || 'dev').replace(':', '-').replaceAll('/', '-') :
    projectVersion;

fs.mkdirSync(outDir, { recursive: true });

esbuild
    .build({
        loader: { '.js': 'jsx' },
        entryPoints: {
            [`js/tomcat-configurer.${indexHtmlVersion}`]: './src/index.js',
        },
        bundle: true,
        watch: dev,
        metafile: dev,
        minify: !dev,
        sourcemap: dev,
        legalComments: 'none',
        logLevel: 'info',
        target: ['chrome58', 'firefox57', 'safari11'],
        outdir: outDir,
        jsxFactory: 'h',
        jsxFragment: 'Fragment',
        inject: ['./esbuild/shim.js'],
        plugins: [
            sassPlugin({
                transform: postcssModules({}),
            }),
        ],
    })
    .catch(e => process.exit(1));

