const gulp = require('gulp')
const babel = require('gulp-babel')
const uglify = require('gulp-uglify')
const cleanCss = require('gulp-clean-css')
const rename = require('gulp-rename')
const replace = require('gulp-replace')
const debug = require('gulp-debug')
const cleanhtml = require('gulp-cleanhtml')
const babelCore = require('@babel/core')
const rev = require('gulp-rev')
const revReplace = require('gulp-rev-replace')

// ES6 > ES5 & compress js 
gulp.task('cjs', () => {
    return gulp.src('../src/main/webapp/assets/js/**/*.js?(x)')
    	.pipe(gulp.dest('./_temp/es6'))
    	.pipe(babel())
    	.pipe(gulp.dest('./_temp/es5'))
    	.pipe(uglify())
    	.pipe(debug({ title: 'compress js file : ' }))
    	//.pipe(rename({ extname: '.min.js' }))
    	.pipe(gulp.dest('./build/assets/js'))
})

// compress css
gulp.task('ccss', () => {
    return gulp.src('../src/main/webapp/assets/css/**/*.css')
    	.pipe(cleanCss())
    	.pipe(debug({ title: 'compress css file : ' }))
    	//.pipe(rename({ extname: '.min.css' }))
    	.pipe(gulp.dest('./build/assets/css'))
})

// replace compressed js/css file in jsp
// compress and replace inline js (babel)
gulp.task('cjsp', () => {
	return gulp.src('../src/main/webapp/**/*.jsp')
		.pipe(debug({ title: 'compress jsp file : ' }))
		.pipe(replace(/<script type="text\/babel">([\s]+[\d\D]*)<\/script>/igm, (m, p, o, s) => {
			p = p.trim()
			if (p.length == 0) return '<script><!--NoCode--></script>'
			let es5 = ''
			try {
				let r = babelCore.transformSync(p, { "presets": ["@babel/env", "@babel/react"], minified: true })
				es5 = r.code
			} catch (err){
				console.log('Babel transform : ' + err)
			}
			return '<script>' + es5 + '</script>'
		}))
		.pipe(replace(/ type="text\/babel"/ig, ''))  // remove type="text/babel"
		.pipe(replace(/\.jsx/g, '.js'))  // replace suffix .jsx > .js
		.pipe(replace('<script src="${baseUrl}/assets/lib/react/babel.js"></script>', ''))  // remove babel lib
		// .pipe(cleanhtml())
		// .pipe(rev())
		// .pipe(revReplace())
		.pipe(gulp.dest('./build'))
})


gulp.task('default', [ 'cjs', 'ccss', 'cjsp' ])
