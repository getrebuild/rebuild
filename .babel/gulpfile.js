const gulp = require('gulp')
const uglify = require('gulp-uglify')
const rename = require('gulp-rename')
const babel = require('gulp-babel')

const SOURCE = '../src/main/webapp/assets/js/**/*.js*'

gulp.task('es62es5', () => {
    return gulp.src(SOURCE)
    	.pipe(gulp.dest('./js/es6'))
    	.pipe(babel())
    	.pipe(gulp.dest('./js/es5'))
    	.pipe(uglify({ mangle: true }))
    	.pipe(rename({ extname: '.min.js' }))
    	.pipe(gulp.dest('./build'))
})

gulp.task('default', ['es62es5'])