const gulp = require('gulp')
const babel = require('gulp-babel')
const uglify = require('gulp-uglify')
const cssclean = require('gulp-clean-css')
const rename = require('gulp-rename')
const replace = require('gulp-replace')
const debug = require('gulp-debug')
const fs = require('graceful-fs')

// compress js (and ES6 > ES5)
gulp.task('es62es5', () => {
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
gulp.task('css', () => {
    return gulp.src('../src/main/webapp/assets/css/**/*.css')
    	.pipe(cssclean())
    	//.pipe(rename({ extname: '.min.css' }))
    	.pipe(gulp.dest('./build/css'))
})

// replace compressed js/css in jsp
// compress and replace inline js
gulp.task('repljsp', () => {
	return gulp.src('../src/main/webapp/**/*-list.jsp')
		// .pipe(replace(/<script type="text\/babel">([\s]+[\d\D]*)<\/script>/igm, (m, p, o, s) => {
		// 	// console.log('ES6 >>>>>>>>>> ' + p)

		// 	let es6_temp = './build/js/temp-es6.js'
		// 	let es5_temp = './build/js/temp-es5.js'
		// 	fs.writeFileSync(es6_temp, p)

		// 	gulp.src(es6_temp)
		// 		// .pipe(babel())
		// 		.pipe(debug({ title: 'compress temp file : ' }))
		// 		// .pipe(uglify({ mangle: true }))
		// 		.pipe(gulp.dest(es5_temp))

		// 	// let es52str = fs.readFileSync(es5_temp)
		// 	// console.log('ES5 >>>>>>>>>> ' + es52str)

		// 	return '<script></script>'
		// }))
		.pipe(replace(/ type="text\/babel"/ig, ''))
		.pipe(gulp.dest('./build/jsp'))
})


gulp.task('default', ['es62es5'])

gulp.task('all', ['es62es5', 'css', 'repljsp'])