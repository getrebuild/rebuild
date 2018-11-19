const gulp = require('gulp')
const babel = require('gulp-babel')
const uglify = require('gulp-uglify')
const cssclean = require('gulp-clean-css')
const rename = require('gulp-rename')
const replace = require('gulp-replace')
const debug = require('gulp-debug')
const fs = require('graceful-fs')
const cleanhtml = require('gulp-cleanhtml')

// compress js (and ES6 > ES5)
gulp.task('xjs', () => {
    return gulp.src('../src/main/webapp/assets/js/**/*.js?(x)')
    	.pipe(gulp.dest('./js/es6'))
    	.pipe(babel())
    	.pipe(gulp.dest('./js/es5'))
    	.pipe(uglify({ mangle: true }))
    	.pipe(debug({ title: 'compress file : ' }))
    	//.pipe(rename({ extname: '.min.js' }))
    	.pipe(gulp.dest('./build/js'))
})

// compress css
gulp.task('xcss', () => {
    return gulp.src('../src/main/webapp/assets/css/**/*.css')
    	.pipe(cssclean())
    	.pipe(debug({ title: 'compress file : ' }))
    	//.pipe(rename({ extname: '.min.css' }))
    	.pipe(gulp.dest('./build/css'))
})

// replace compressed js/css in jsp
// compress and replace inline js
gulp.task('xjsp', () => {
	return gulp.src('../src/main/webapp/**/*.jsp')
		.pipe(debug({ title: 'compress file : ' }))
		.pipe(replace(/<script type="text\/babel">([\s]+[\d\D]*)<\/script>/igm, (m, p, o, s) => {
			// console.log('ES6 >>>>>>>>>> ' + p)

			let tmp = '__temp.js'
			fs.unlinkSync(tmp)

			fs.writeFileSync(tmp, p)
			gulp.src(tmp)
				.pipe(babel())
				.pipe(uglify({ mangle: true }))
				.pipe(gulp.dest('./build'))

			let es5 = fs.readFileSync('./build/' + tmp)
			// console.log('ES5 >>>>>>>>>> ' + es5)
			// fs.unlinkSync(tmp)

			return '<script>' + es5 + '</script>'
		}))
		.pipe(replace(/ type="text\/babel"/ig, ''))  // remove type="text/babel"
		.pipe(replace(/\.jsx"><\/script>/ig, '.js"></script>'))  // replace suffix .jsx > .js
		.pipe(replace('<script src="${baseUrl}/assets/lib/react/babel.js"></script>', ''))  // remove babel lib
		// .pipe(cleanhtml())
		.pipe(gulp.dest('./build/jsp'))
})


gulp.task('default', ['xjs'])
gulp.task('all', ['xjs', 'xcss', 'xjsp'])
