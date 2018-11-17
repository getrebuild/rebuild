const gulp = require('gulp')
const babel = require('gulp-babel')
const uglify = require('gulp-uglify')
const cssclean = require('gulp-clean-css')
const rename = require('gulp-rename')


gulp.task('es62es5', () => {
    return gulp.src('../src/main/webapp/assets/js/**/*.js*')
    	.pipe(gulp.dest('./js/es6'))
    	.pipe(babel())
    	.pipe(gulp.dest('./js/es5'))
    	.pipe(uglify({ mangle: true }))
    	.pipe(rename({ extname: '.min.js' }))
    	.pipe(gulp.dest('./build/js'))
})

gulp.task('css', () => {
    return gulp.src('../src/main/webapp/assets/css/**/*.css')
    	.pipe(cssclean())
    	.pipe(rename({ extname: '.min.css' }))
    	.pipe(gulp.dest('./build/css'))
})

gulp.task('default', ['es62es5'])

gulp.task('all', ['es62es5', 'css'])