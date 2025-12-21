!function(t,i){"object"==typeof exports&&"undefined"!=typeof module?module.exports=i():"function"==typeof define&&define.amd?define(i):(t="undefined"!=typeof globalThis?globalThis:t||self).Cluster=i()}(this,(function(){"use strict";class t{constructor(i){this.t=null!=i?i:t.guid()}getHandlers(t){let i=this.i||(this.i=new Map);return i.has(t)||i.set(t,new Map),i.get(t)}on(i,e){"function"==typeof e&&this.getHandlers(i).set(e,t.guid())}addEventListener(t,i){this.on(t,i)}off(t,i){var e;let s=this.getHandlers(t);i?s.has(i)&&s.delete(i):null===(e=this.i)||void 0===e||e.clear()}removeEventListener(t,i){this.off(t,i)}fire(t,i){let e=Array.from(this.getHandlers(t).entries());for(const[t]of e)t.call(this,i)}dispatchEvent(t,i){this.fire(t,i)}destroy(){var t;null===(t=this.i)||void 0===t||t.clear(),this.i=null}get hashCode(){return this.t}static guid(){return"xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx".replace(/[xy]/g,(function(t){let i=16*Math.random()|0;return("x"==t?i:3&i|8).toString(16)}))}}var i,e,s,r,n,h;!function(t){t.CLICK="click",t.MOUSE_OVER="mouseover",t.MOUSE_OUT="mouseout",t.CHANGE="change",t.DESTROY="destroy"}(i||(i={})),function(t){t.DIS_PIXEL="dis-pixel",t.ATTR_REF="attribute",t.GEO_FENCE="geo-fence"}(e||(e={})),function(t){t[t.SUM=1]="SUM",t[t.AVERAGE=2]="AVERAGE",t[t.MAX=4]="MAX"}(s||(s={})),function(t){t[t.AVERAGE=1]="AVERAGE",t[t.COUNT_AVERAGE=3]="COUNT_AVERAGE",t[t.WEIGHT_AVERAGE=5]="WEIGHT_AVERAGE",t[t.WEIGHT_MAX=4]="WEIGHT_MAX"}(r||(r={})),function(t){t.DOM="dom",t.WEBGL="webgl"}(n||(n={})),function(t){t.MULTI="multi",t.SINGLE="single"}(h||(h={}));const o=[Int8Array,Uint8Array,Uint8ClampedArray,Int16Array,Uint16Array,Int32Array,Uint32Array,Float32Array,Float64Array];
/** @typedef {Int8ArrayConstructor | Uint8ArrayConstructor | Uint8ClampedArrayConstructor | Int16ArrayConstructor | Uint16ArrayConstructor | Int32ArrayConstructor | Uint32ArrayConstructor | Float32ArrayConstructor | Float64ArrayConstructor} TypedArrayConstructor */class l{
/**
       * Creates an index from raw `ArrayBuffer` data.
       * @param {ArrayBuffer} data
       */
static from(t){if(!(t instanceof ArrayBuffer))throw new Error("Data must be an instance of ArrayBuffer.");const[i,e]=new Uint8Array(t,0,2);if(219!==i)throw new Error("Data does not appear to be in a KDBush format.");const s=e>>4;if(1!==s)throw new Error(`Got v${s} data when expected v1.`);const r=o[15&e];if(!r)throw new Error("Unrecognized array type.");const[n]=new Uint16Array(t,2,1),[h]=new Uint32Array(t,4,1);return new l(h,n,r,t)}
/**
       * Creates an index that will hold a given number of items.
       * @param {number} numItems
       * @param {number} [nodeSize=64] Size of the KD-tree node (64 by default).
       * @param {TypedArrayConstructor} [ArrayType=Float64Array] The array type used for coordinates storage (`Float64Array` by default).
       * @param {ArrayBuffer} [data] (For internal use only)
       */constructor(t,i=64,e=Float64Array,s){if(isNaN(t)||t<0)throw new Error(`Unpexpected numItems value: ${t}.`);this.numItems=+t,this.nodeSize=Math.min(Math.max(+i,2),65535),this.ArrayType=e,this.IndexArrayType=t<65536?Uint16Array:Uint32Array;const r=o.indexOf(this.ArrayType),n=2*t*this.ArrayType.BYTES_PER_ELEMENT,h=t*this.IndexArrayType.BYTES_PER_ELEMENT,l=(8-h%8)%8;if(r<0)throw new Error(`Unexpected typed array class: ${e}.`);s&&s instanceof ArrayBuffer?(
// reconstruct an index from a buffer
this.data=s,this.ids=new this.IndexArrayType(this.data,8,t),this.coords=new this.ArrayType(this.data,8+h+l,2*t),this.h=2*t,this.o=!0):(
// initialize a new index
this.data=new ArrayBuffer(8+n+h+l),this.ids=new this.IndexArrayType(this.data,8,t),this.coords=new this.ArrayType(this.data,8+h+l,2*t),this.h=0,this.o=!1,
// set header
new Uint8Array(this.data,0,2).set([219,16+r]),new Uint16Array(this.data,2,1)[0]=i,new Uint32Array(this.data,4,1)[0]=t)}
/**
       * Add a point to the index.
       * @param {number} x
       * @param {number} y
       * @returns {number} An incremental index associated with the added item (starting from `0`).
       */add(t,i){const e=this.h>>1;return this.ids[e]=e,this.coords[this.h++]=t,this.coords[this.h++]=i,e}
/**
       * Perform indexing of the added points.
       */finish(){const t=this.h>>1;if(t!==this.numItems)throw new Error(`Added ${t} items when expected ${this.numItems}.`);
// kd-sort both arrays for efficient search
return a(this.ids,this.coords,this.nodeSize,0,this.numItems-1,0),this.o=!0,this}
/**
       * Search the index for items within a given bounding box.
       * @param {number} minX
       * @param {number} minY
       * @param {number} maxX
       * @param {number} maxY
       * @returns {number[]} An array of indices correponding to the found items.
       */range(t,i,e,s){if(!this.o)throw new Error("Data not yet indexed - call index.finish().");const{ids:r,coords:n,nodeSize:h}=this,o=[0,r.length-1,0],l=[];
// recursively search for items in range in the kd-sorted arrays
for(;o.length;){const a=o.pop()||0,u=o.pop()||0,c=o.pop()||0;
// if we reached "tree node", search linearly
if(u-c<=h){for(let h=c;h<=u;h++){const o=n[2*h],a=n[2*h+1];o>=t&&o<=e&&a>=i&&a<=s&&l.push(r[h])}continue}
// otherwise find the middle index
const f=c+u>>1,d=n[2*f],p=n[2*f+1];
// include the middle item if it's in range
d>=t&&d<=e&&p>=i&&p<=s&&l.push(r[f]),
// queue search in halves that intersect the query
(0===a?t<=d:i<=p)&&(o.push(c),o.push(f-1),o.push(1-a)),(0===a?e>=d:s>=p)&&(o.push(f+1),o.push(u),o.push(1-a))}return l}
/**
       * Search the index for items within a given radius.
       * @param {number} qx
       * @param {number} qy
       * @param {number} r Query radius.
       * @returns {number[]} An array of indices correponding to the found items.
       */within(t,i,e){if(!this.o)throw new Error("Data not yet indexed - call index.finish().");const{ids:s,coords:r,nodeSize:n}=this,h=[0,s.length-1,0],o=[],l=e*e;
// recursively search for items within radius in the kd-sorted arrays
for(;h.length;){const a=h.pop()||0,u=h.pop()||0,c=h.pop()||0;
// if we reached "tree node", search linearly
if(u-c<=n){for(let e=c;e<=u;e++)d(r[2*e],r[2*e+1],t,i)<=l&&o.push(s[e]);continue}
// otherwise find the middle index
const f=c+u>>1,p=r[2*f],y=r[2*f+1];
// include the middle item if it's in range
d(p,y,t,i)<=l&&o.push(s[f]),
// queue search in halves that intersect the query
(0===a?t-e<=p:i-e<=y)&&(h.push(c),h.push(f-1),h.push(1-a)),(0===a?t+e>=p:i+e>=y)&&(h.push(f+1),h.push(u),h.push(1-a))}return o}}
/**
     * @param {Uint16Array | Uint32Array} ids
     * @param {InstanceType<TypedArrayConstructor>} coords
     * @param {number} nodeSize
     * @param {number} left
     * @param {number} right
     * @param {number} axis
     */function a(t,i,e,s,r,n){if(r-s<=e)return;const h=s+r>>1;// middle index
// sort ids and coords around the middle index so that the halves lie
// either left/right or top/bottom correspondingly (taking turns)
u(t,i,h,s,r,n),
// recursively kd-sort first half and second half on the opposite axis
a(t,i,e,s,h-1,1-n),a(t,i,e,h+1,r,1-n)}
/**
     * Custom Floyd-Rivest selection algorithm: sort ids and coords so that
     * [left..k-1] items are smaller than k-th item (on either x or y axis)
     * @param {Uint16Array | Uint32Array} ids
     * @param {InstanceType<TypedArrayConstructor>} coords
     * @param {number} k
     * @param {number} left
     * @param {number} right
     * @param {number} axis
     */function u(t,i,e,s,r,n){for(;r>s;){if(r-s>600){const h=r-s+1,o=e-s+1,l=Math.log(h),a=.5*Math.exp(2*l/3),c=.5*Math.sqrt(l*a*(h-a)/h)*(o-h/2<0?-1:1);u(t,i,e,Math.max(s,Math.floor(e-o*a/h+c)),Math.min(r,Math.floor(e+(h-o)*a/h+c)),n)}const h=i[2*e+n];let o=s,l=r;for(c(t,i,s,e),i[2*r+n]>h&&c(t,i,s,r);o<l;){for(c(t,i,o,l),o++,l--;i[2*o+n]<h;)o++;for(;i[2*l+n]>h;)l--}i[2*s+n]===h?c(t,i,s,l):(l++,c(t,i,l,r)),l<=e&&(s=l+1),e<=l&&(r=l-1)}}
/**
     * @param {Uint16Array | Uint32Array} ids
     * @param {InstanceType<TypedArrayConstructor>} coords
     * @param {number} i
     * @param {number} j
     */function c(t,i,e,s){f(t,e,s),f(i,2*e,2*s),f(i,2*e+1,2*s+1)}
/**
     * @param {InstanceType<TypedArrayConstructor>} arr
     * @param {number} i
     * @param {number} j
     */function f(t,i,e){const s=t[i];t[i]=t[e],t[e]=s}
/**
     * @param {number} ax
     * @param {number} ay
     * @param {number} bx
     * @param {number} by
     */function d(t,i,e,s){const r=t-e,n=i-s;return r*r+n*n}
/**
     * @module helpers
     */
/**
     * Earth Radius used with the Harvesine formula and approximates using a spherical (non-ellipsoid) Earth.
     *
     * @memberof helpers
     * @type {number}
     */
/**
     * Wraps a GeoJSON {@link Geometry} in a GeoJSON {@link Feature}.
     *
     * @name feature
     * @param {Geometry} geometry input geometry
     * @param {Object} [properties={}] an Object of key-value pairs to add as properties
     * @param {Object} [options={}] Optional Parameters
     * @param {Array<number>} [options.bbox] Bounding Box Array [west, south, east, north] associated with the Feature
     * @param {string|number} [options.id] Identifier associated with the Feature
     * @returns {Feature} a GeoJSON Feature
     * @example
     * var geometry = {
     *   "type": "Point",
     *   "coordinates": [110, 50]
     * };
     *
     * var feature = turf.feature(geometry);
     *
     * //=feature
     */
/**
     * isNumber
     *
     * @param {*} num Number to validate
     * @returns {boolean} true/false
     * @example
     * turf.isNumber(123)
     * //=true
     * turf.isNumber('foo')
     * //=false
     */
function p(t){return!isNaN(t)&&null!==t&&!Array.isArray(t)}
/**
     * Callback for coordEach
     *
     * @callback coordEachCallback
     * @param {Array<number>} currentCoord The current coordinate being processed.
     * @param {number} coordIndex The current index of the coordinate being processed.
     * @param {number} featureIndex The current index of the Feature being processed.
     * @param {number} multiFeatureIndex The current index of the Multi-Feature being processed.
     * @param {number} geometryIndex The current index of the Geometry being processed.
     */
/**
     * Iterate over coordinates in any GeoJSON object, similar to Array.forEach()
     *
     * @name coordEach
     * @param {FeatureCollection|Feature|Geometry} geojson any GeoJSON object
     * @param {Function} callback a method that takes (currentCoord, coordIndex, featureIndex, multiFeatureIndex)
     * @param {boolean} [excludeWrapCoord=false] whether or not to include the final coordinate of LinearRings that wraps the ring in its iteration.
     * @returns {void}
     * @example
     * var features = turf.featureCollection([
     *   turf.point([26, 37], {"foo": "bar"}),
     *   turf.point([36, 53], {"hello": "world"})
     * ]);
     *
     * turf.coordEach(features, function (currentCoord, coordIndex, featureIndex, multiFeatureIndex, geometryIndex) {
     *   //=currentCoord
     *   //=coordIndex
     *   //=featureIndex
     *   //=multiFeatureIndex
     *   //=geometryIndex
     * });
     */function y(t,i,e){
// Handles null Geometry -- Skips this GeoJSON
if(null!==t)
// This logic may look a little weird. The reason why it is that way
// is because it's trying to be fast. GeoJSON supports multiple kinds
// of objects at its root: FeatureCollection, Features, Geometries.
// This function has the responsibility of handling all of them, and that
// means that some of the `for` loops you see below actually just don't apply
// to certain inputs. For instance, if you give this just a
// Point geometry, then both loops are short-circuited and all we do
// is gradually rename the input until it's called 'geometry'.
// This also aims to allocate as few resources as possible: just a
// few numbers and booleans, rather than any temporary arrays as would
// be required with the normalization approach.
for(var s,r,n,h,o,l,a,u,c=0,f=0,d=t.type,p="FeatureCollection"===d,m="Feature"===d,w=p?t.features.length:1,v=0;v<w;v++){o=(u=!!(a=p?t.features[v].geometry:m?t.geometry:t)&&"GeometryCollection"===a.type)?a.geometries.length:1;for(var g=0;g<o;g++){var M=0,b=0;
// Handles null Geometry -- Skips this geometry
if(null!==(h=u?a.geometries[g]:a)){l=h.coordinates;var x=h.type;switch(c=!e||"Polygon"!==x&&"MultiPolygon"!==x?0:1,x){case null:break;case"Point":if(!1===i(l,f,v,M,b))return!1;f++,M++;break;case"LineString":case"MultiPoint":for(s=0;s<l.length;s++){if(!1===i(l[s],f,v,M,b))return!1;f++,"MultiPoint"===x&&M++}"LineString"===x&&M++;break;case"Polygon":case"MultiLineString":for(s=0;s<l.length;s++){for(r=0;r<l[s].length-c;r++){if(!1===i(l[s][r],f,v,M,b))return!1;f++}"MultiLineString"===x&&M++,"Polygon"===x&&b++}"Polygon"===x&&M++;break;case"MultiPolygon":for(s=0;s<l.length;s++){for(b=0,r=0;r<l[s].length;r++){for(n=0;n<l[s][r].length-c;n++){if(!1===i(l[s][r][n],f,v,M,b))return!1;f++}b++}M++}break;case"GeometryCollection":for(s=0;s<h.geometries.length;s++)if(!1===y(h.geometries[s],i,e))return!1;break;default:throw new Error("Unknown Geometry Type")}}}}}
/**
     * Takes a set of features, calculates the bbox of all input features, and returns a bounding box.
     *
     * @name bbox
     * @param {GeoJSON} geojson any GeoJSON object
     * @returns {BBox} bbox extent in [minX, minY, maxX, maxY] order
     * @example
     * var line = turf.lineString([[-74, 40], [-78, 42], [-82, 35]]);
     * var bbox = turf.bbox(line);
     * var bboxPolygon = turf.bboxPolygon(bbox);
     *
     * //addToMap
     * var addToMap = [line, bboxPolygon]
     */function m(t){var i=[1/0,1/0,-1/0,-1/0];return y(t,(function(t){i[0]>t[0]&&(i[0]=t[0]),i[1]>t[1]&&(i[1]=t[1]),i[2]<t[0]&&(i[2]=t[0]),i[3]<t[1]&&(i[3]=t[1])})),i}
// http://en.wikipedia.org/wiki/Even%E2%80%93odd_rule
// modified from: https://github.com/substack/point-in-polygon/blob/master/index.js
// which was modified from http://www.ecse.rpi.edu/Homepages/wrf/Research/Short_Notes/pnpoly.html
/**
     * Takes a {@link Point} and a {@link Polygon} or {@link MultiPolygon} and determines if the point
     * resides inside the polygon. The polygon can be convex or concave. The function accounts for holes.
     *
     * @name booleanPointInPolygon
     * @param {Coord} point input point
     * @param {Feature<Polygon|MultiPolygon>} polygon input polygon or multipolygon
     * @param {Object} [options={}] Optional parameters
     * @param {boolean} [options.ignoreBoundary=false] True if polygon boundary should be ignored when determining if
     * the point is inside the polygon otherwise false.
     * @returns {boolean} `true` if the Point is inside the Polygon; `false` if the Point is not inside the Polygon
     * @example
     * var pt = turf.point([-77, 44]);
     * var poly = turf.polygon([[
     *   [-81, 41],
     *   [-81, 47],
     *   [-72, 47],
     *   [-72, 41],
     *   [-81, 41]
     * ]]);
     *
     * turf.booleanPointInPolygon(pt, poly);
     * //= true
     */
function w(t,i,e){
// validation
if(void 0===e&&(e={}),!t)throw new Error("point is required");if(!i)throw new Error("polygon is required");var s,r=
/**
     * Unwrap a coordinate from a Point Feature, Geometry or a single coordinate.
     *
     * @name getCoord
     * @param {Array<number>|Geometry<Point>|Feature<Point>} coord GeoJSON Point or an Array of numbers
     * @returns {Array<number>} coordinates
     * @example
     * var pt = turf.point([10, 10]);
     *
     * var coord = turf.getCoord(pt);
     * //= [10, 10]
     */
function(t){if(!t)throw new Error("coord is required");if(!Array.isArray(t)){if("Feature"===t.type&&null!==t.geometry&&"Point"===t.geometry.type)return t.geometry.coordinates;if("Point"===t.type)return t.coordinates}if(Array.isArray(t)&&t.length>=2&&!Array.isArray(t[0])&&!Array.isArray(t[1]))return t;throw new Error("coord must be GeoJSON Point or an Array of numbers")}
/**
     * Get Geometry from Feature or Geometry Object
     *
     * @param {Feature|Geometry} geojson GeoJSON Feature or Geometry Object
     * @returns {Geometry|null} GeoJSON Geometry Object
     * @throws {Error} if geojson is not a Feature or Geometry Object
     * @example
     * var point = {
     *   "type": "Feature",
     *   "properties": {},
     *   "geometry": {
     *     "type": "Point",
     *     "coordinates": [110, 40]
     *   }
     * }
     * var geom = turf.getGeom(point)
     * //={"type": "Point", "coordinates": [110, 40]}
     */(t),n="Feature"===(s=i).type?s.geometry:s,h=n.type,o=i.bbox,l=n.coordinates;
// Quick elimination if point is not inside bbox
if(o&&!1===
/**
     * inBBox
     *
     * @private
     * @param {Position} pt point [x,y]
     * @param {BBox} bbox BBox [west, south, east, north]
     * @returns {boolean} true/false if point is inside BBox
     */
function(t,i){return i[0]<=t[0]&&i[1]<=t[1]&&i[2]>=t[0]&&i[3]>=t[1]}
/**
     * Takes one or more features and calculates the centroid using the mean of all vertices.
     * This lessens the effect of small islands and artifacts when calculating the centroid of a set of polygons.
     *
     * @name centroid
     * @param {GeoJSON} geojson GeoJSON to be centered
     * @param {Object} [options={}] Optional Parameters
     * @param {Object} [options.properties={}] an Object that is used as the {@link Feature}'s properties
     * @returns {Feature<Point>} the centroid of the input features
     * @example
     * var polygon = turf.polygon([[[-81, 41], [-88, 36], [-84, 31], [-80, 33], [-77, 39], [-81, 41]]]);
     *
     * var centroid = turf.centroid(polygon);
     *
     * //addToMap
     * var addToMap = [polygon, centroid]
     */(r,o))return!1;
// normalize to multipolygon
"Polygon"===h&&(l=[l]);for(var a=!1,u=0;u<l.length&&!a;u++)
// check if it is in the outer ring first
if(v(r,l[u][0],e.ignoreBoundary)){
// check for the point in any of the holes
for(var c=!1,f=1;f<l[u].length&&!c;)v(r,l[u][f],!e.ignoreBoundary)&&(c=!0),f++;c||(a=!0)}return a}
/**
     * inRing
     *
     * @private
     * @param {Array<number>} pt [x,y]
     * @param {Array<Array<number>>} ring [[x,y], [x,y],..]
     * @param {boolean} ignoreBoundary ignoreBoundary
     * @returns {boolean} inRing
     */function v(t,i,e){var s=!1;i[0][0]===i[i.length-1][0]&&i[0][1]===i[i.length-1][1]&&(i=i.slice(0,i.length-1));for(var r=0,n=i.length-1;r<i.length;n=r++){var h=i[r][0],o=i[r][1],l=i[n][0],a=i[n][1];if(t[1]*(h-l)+o*(l-t[0])+a*(t[0]-h)==0&&(h-t[0])*(l-t[0])<=0&&(o-t[1])*(a-t[1])<=0)return!e;o>t[1]!=a>t[1]&&t[0]<(l-h)*(t[1]-o)/(a-o)+h&&(s=!s)}return s}m.default=m;const g=(t,i,e)=>{let s,r,n,h=null,o=0;e||(e={});let l=function(){o=!1===e.leading?0:Date.now(),h=null,n=t.apply(r,s),h||(r=s=null)};return function(){let a=Date.now();o||!1!==e.leading||(o=a);let u=i-(a-o);return r=this,s=arguments,u<=0||u>i?(h&&(clearTimeout(h),h=null),o=a,n=t.apply(r,s),h||(r=s=null)):h||!1===e.trailing||(h=setTimeout(l,u)),n}},M=Math.fround||(b=new Float32Array(1),t=>(b[0]=+t,b[0]));var b;const x=(t,i,e)=>t.reduce(((t,s,r)=>{let n=i(s);return n=null==n||""===n?"undefined":n,t[n]||(t[n]=[]),e.type&&e.zoom&&(s["_inner_"+e.type+"_"+e.zoom]=n),s.id=r,t[n].push(s),t}),{}),A=t=>({fence:!0,bbox:m(t),center:L(t)}),E=(t,i)=>m(i?{type:"FeatureCollection",features:[{type:"Feature",properties:{},geometry:{type:"Polygon",coordinates:[t]}}]}:{type:"FeatureCollection",features:t}),L=t=>function(t,i){void 0===i&&(i={});var e=0,s=0,r=0;return y(t,(function(t){e+=t[0],s+=t[1],r++}),!0),
/**
     * Creates a {@link Point} {@link Feature} from a Position.
     *
     * @name point
     * @param {Array<number>} coordinates longitude, latitude position (each in decimal degrees)
     * @param {Object} [properties={}] an Object of key-value pairs to add as properties
     * @param {Object} [options={}] Optional Parameters
     * @param {Array<number>} [options.bbox] Bounding Box Array [west, south, east, north] associated with the Feature
     * @param {string|number} [options.id] Identifier associated with the Feature
     * @returns {Feature<Point>} a Point feature
     * @example
     * var point = turf.point([-75.343, 39.984]);
     *
     * //=point
     */
function(t,i,e){if(void 0===e&&(e={}),!t)throw new Error("coordinates is required");if(!Array.isArray(t))throw new Error("coordinates must be an Array");if(t.length<2)throw new Error("coordinates must be at least 2 numbers long");if(!p(t[0])||!p(t[1]))throw new Error("coordinates must contain numbers");return function(t,i,e){void 0===e&&(e={});var s={type:"Feature"};return(0===e.id||e.id)&&(s.id=e.id),e.bbox&&(s.bbox=e.bbox),s.properties=i||{},s.geometry=t,s}({type:"Point",coordinates:t},i,e)}([e/r,s/r],i.properties)}(t).geometry.coordinates,C=t=>{if(!t||!Array.isArray(t)||t.length<2)return[];let i=[t[0],t[1]];for(let e=2;e<t.length-1;e+=2)t[e]!==i[e-2]&&t[e+1]!==i[e-1]&&i.push(t[e],t[e+1]);return i},k=t=>t/360+.5,_=t=>{const i=Math.sin(t*Math.PI/180),e=.5-.25*Math.log((1+i)/(1-i))/Math.PI;return e<0?0:e>1?1:e};class T{constructor(){}}var G;!function(t){t[t.NONE=0]="NONE",t[t.INNER=1]="INNER",t[t.OUT=2]="OUT",t[t.ALL=3]="ALL"}(G||(G={}));class B{constructor(t,e=!1){this.trees={},this.nodeSize=64,this.isSort=!0,this.splitChar="*.*",this.OFFSET_ZOOM=2,this.OFFSET_ID=3,this.OFFSET_PARENT=4,this.OFFSET_NUM=5,this.OFFSET_PROP=6,this.ZOOM_BITS=5,this.mapping=new Map,this.geo_refs=new Map,this.key_refs=new Map,this.cluster_geo_refs=new Map,this.own=t,this.showLog=e,this.reduceType=this.isClusterReduce(),this.stride=this.reduceType!==G.NONE?7:6,this.reduceType&G.INNER&&(this.innerReduce=(t,i)=>{var e,s,r;this.weightKey&&(t.weight_list||(t.weight_list=[null!==(e=t.weight)&&void 0!==e?e:1]),t.weight_list.push(null!==(s=null==i?void 0:i.weight)&&void 0!==s?s:1)),this.typeKey&&(t.divide_type=null!==(r=null==i?void 0:i.divide_type)&&void 0!==r?r:"unknown")},this.innerMap=t=>{var i,e,s,r;let n={};return this.weightKey&&("function"==typeof this.weightKey?n.weight=null!==(i=this.weightKey(t))&&void 0!==i?i:1:n.weight=t?null!==(e=t[this.weightKey])&&void 0!==e?e:1:null),this.typeKey&&("function"==typeof this.typeKey?n.divide_type=null!==(s=this.typeKey(t))&&void 0!==s?s:"unknown":n.divide_type=t?null!==(r=t[this.typeKey])&&void 0!==r?r:"unknown":null),n}),this.createZoomMapping(),this.own.on(i.DESTROY,(()=>{this.reset(),this.mapping.clear()}))}isClusterReduce(){let t=this.own.getOptions().clusterPointWeight,i=this.own.getOptions().clusterPointType;return t||i?(t&&(this.weightKey=t),i&&(this.typeKey=i),this.own.getOptions().clusterReduce?G.ALL:G.INNER):this.own.getOptions().clusterReduce?G.OUT:G.NONE}createZoomMapping(){let t=this.own.getOptions().clusterType;t&&0!==t.length&&(t.forEach((t=>{let[i,s,r,n,h]=t;if(s=null!=s?s:this.own.getOptions().clusterMaxZoom,r===e.GEO_FENCE||r===e.ATTR_REF)this.mapping.set(i,[null!=h?h:s,r,n]);else if(h)this.mapping.set(i,[h,e.DIS_PIXEL,n]);else for(var o=i;o<=s;o++)this.mapping.set(o,[o,e.DIS_PIXEL,n])})),this.showLog)}getClusterType(t){let i,s=e.DIS_PIXEL;for(let[r,n]of[...this.mapping.entries()].reverse())if(r<=t){s=n[1],i=s===e.ATTR_REF&&n[2]instanceof Array?n[2][n[2].length-1]:n[2];break}return{type:s,name:i}}getClusterZoom(t){let i=this.l(t);for(let[e]of[...this.mapping.entries()].reverse())if(e<=t){i=e;break}return i}getClusterTree(t){let i=this.getClusterZoom(t);const{clusterMaxZoom:e=21}=this.own.getOptions();return i<t&&e+1===t&&(i=e+1),this.trees[i]}createClusters(t){this.reset(),this.points=t.filter((t=>{if(t.geometry){const[i,e]=t.geometry.coordinates;if(!isNaN(i)&&!isNaN(e)&&i>=-180&&i<=180&&e>=-90&&e<=90)return!0}return!1}));const{clusterMinZoom:i=3,clusterMaxZoom:s=21}=this.own.getOptions();t.length,this.showLog;const r=[];for(let t=0;t<this.points.length;t++){const i=this.points[t];if(!i.geometry)continue;const[e,s]=i.geometry.coordinates,n=M(e),h=M(s);r.push(n,h,1/0,t,-1,1),this.reduceType!==G.NONE&&r.push(-1)}let n=this.trees[s+1]=this.u(new Float32Array(r));for(let t=s;t>=i;t--){Date.now();const i=this.getClusterZoom(t);if(this.mapping.has(i)){if(this.trees[i])continue;let t,[s,r,h]=this.mapping.get(i);switch(r){case e.GEO_FENCE:t=this.p(s,h);break;case e.ATTR_REF:t=this.m(s,h);break;default:t=this.v(n,s,h)}n=this.trees[i]=this.u(t)}else n=this.trees[i]=this.u(this.v(n,i));this.showLog}this.showLog}u(t){const i=new l(t.length/this.stride|0,this.nodeSize,Float32Array);for(let e=0;e<t.length;e+=this.stride)i.add(k(t[e]),_(t[e+1]));return i.finish(),i.data=t,i}m(t,i){let s=x(this.points,(t=>{if(!t.properties)return null;let s=null;if(i instanceof Array?i.forEach((i=>{var e;(null===(e=t.properties)||void 0===e?void 0:e.hasOwnProperty(i))&&(null===s?s=t.properties[i]:s+=this.splitChar+t.properties[i])})):s=t.properties[i],this.own.getOptions().clusterDictionary&&s&&!this.geo_refs.has(s)){let t=this.own.getOptions().clusterDictionary(e.ATTR_REF,s);t&&t.point&&this.geo_refs.set(s,t)}return s}),{zoom:t,type:e.ATTR_REF});return this.showLog,this.M(e.ATTR_REF,s,t)}p(t,i){let s=new Map;if(this.own.getOptions().clusterDictionary){let t=i;t instanceof Array||(t=[t]),t.forEach((t=>{if(this.geo_refs.has(t))s.set(t,this.geo_refs.get(t));else{this.showLog;let i=this.own.getOptions().clusterDictionary(e.GEO_FENCE,t);i&&i.region&&(i.point||(i.point=L(i.region)),this.geo_refs.set(t,i),s.set(t,i))}}))}let r=x(this.points,(t=>((t,i,e)=>{let s=null;for(let[e,r]of t)if(w(i,{type:"Feature",geometry:{type:"Polygon",coordinates:[r.region]}})){s=e;break}return s})(s,t)),{zoom:t,type:e.GEO_FENCE});return this.showLog,this.M(e.GEO_FENCE,r,t)}M(t,i,s){let r=[];const{clusterMinPoints:n=3,clusterReduce:h}=this.own.getOptions();for(const o in i)if(i.hasOwnProperty(o)){let l=i[o];if("undefined"===o||l.length<n)l.forEach((t=>{var i;const[e,s]=t.geometry.coordinates,n=e,h=s;r.push(n,h,1/0,+(null!==(i=t.id)&&void 0!==i?i:0),-1,1),this.reduceType!==G.NONE&&r.push(-1)}));else{let i,a=-1;l.forEach(((t,e)=>{if(this.reduceType!==G.NONE){let s={};if(this.reduceType&G.INNER&&(s=this.innerMap(t.properties)),h){let i=this.own.getOptions().clusterMap(t.properties);Object.assign(s,i)}i?(this.reduceType&G.INNER&&(this.innerReduce(i,Object.assign({},s)),e===l.length-1&&this.A(i)),h&&h(i,Object.assign({},s))):(i=Object.assign({},s),a=this.clusterProps.length,this.clusterProps.push(i))}}));let u=(l[0].id<<this.ZOOM_BITS)+(s+1)+this.points.length,c=o.split(this.splitChar);if(this.key_refs.set(u,c[c.length-1]),this.geo_refs.has(o)){let i=this.geo_refs.get(o),s=i.point;r.push(s[0],s[1],1/0,u,-1,l.length),t===e.GEO_FENCE?this.cluster_geo_refs.set(u,{bbox:E(i.region,!0)}):this.cluster_geo_refs.set(u,{bbox:E(l,!1)})}else{let{fence:t,bbox:i,center:e}=A({type:"FeatureCollection",features:l});C(i).length<=2||i[0]===1/0||!t?(n>1&&(u=l[0].id),r.push(e[0],e[1],1/0,u,-1,1)):r.push(e[0],e[1],1/0,u,-1,l.length),this.cluster_geo_refs.set(u,{bbox:i})}this.reduceType!==G.NONE&&r.push(a)}}return r}v(t,i,e){var s,r,n;const{clusterRadius:h=60,tileSize:o=256,clusterMinPoints:l=3,clusterReduce:a}=this.own.getOptions(),u=(void 0===e?h:e)/(o*Math.pow(2,i-1)),c=t.data,f=[],d=this.stride;for(let e=0;e<c.length;e+=d){if(c[e+this.OFFSET_ZOOM]<=i)continue;c[e+this.OFFSET_ZOOM]=i;const h=c[e],o=c[e+1];let p=t.within(k(c[e]),_(c[e+1]),u);if(this.showLog,this.typeKey){let t=null!==(r=null===(s=this.L(c,e))||void 0===s?void 0:s.divide_type)&&void 0!==r?r:"unknown";p=p.filter((i=>{var e,s;const r=i*d;let n=null!==(s=null===(e=this.L(c,r))||void 0===e?void 0:e.divide_type)&&void 0!==s?s:"unknown";return n!=t&&this.showLog,n===t}))}this.showLog;const y=c[e+this.OFFSET_NUM];let m=y;for(const t of p){const e=t*d;c[e+this.OFFSET_ZOOM]>i&&(m+=c[e+this.OFFSET_NUM])}if(m>y&&m>=l){let t,s=-1,r=[[h,o]],l=[y];this.reduceType!==G.NONE&&(t||(t=this.L(c,e,!0),s=this.clusterProps.length,this.clusterProps.push(t)));const u=(e/d<<this.ZOOM_BITS)+(i+1)+this.points.length;for(let e=0;e<p.length;++e){const s=p[e]*d;if(c[s+this.OFFSET_ZOOM]<=i)continue;let n;c[s+this.OFFSET_ZOOM]=i,this.reduceType!==G.NONE&&t&&(n=this.L(c,s),this.reduceType&G.INNER&&this.innerReduce(t,n),a&&a(t,n));let h=c[s+this.OFFSET_NUM];l.push(h),r.push([c[s],c[s+1]]),c[s+this.OFFSET_PARENT]=u}c[e+this.OFFSET_PARENT]=u,this.cluster_geo_refs.set(u,{bbox:E(r,!0)});let[w,v]=this.C(r,l,null!==(n=null==t?void 0:t.weight_list)&&void 0!==n?n:[]);f.push(w,v,1/0,u,-1,m),this.reduceType&G.INNER&&t&&this.weightKey&&this.A(t),this.reduceType!==G.NONE&&f.push(s)}else{for(let t=0;t<d;t++)f.push(c[e+t]);if(m>1)for(const t of p){const e=t*d;if(!(c[e+this.OFFSET_ZOOM]<=i)){c[e+this.OFFSET_ZOOM]=i;for(let t=0;t<d;t++)f.push(c[e+t])}}}}return f}C(t,i,e){let s=0,n=0,h=0,o=this.own.getOptions().clusterPointLocationType;if(0===e.length)o=r.COUNT_AVERAGE;else if(t.length!==e.length)throw new Error("weight_list length not equal clusterPoints length");switch(o){case r.WEIGHT_MAX:let o=e.reduce(((t,i,s)=>i>e[t]?s:t),0);s=t[o][0],n=t[o][1];break;case r.WEIGHT_AVERAGE:h=0,e.forEach((t=>{h+=t}));for(let i=0;i<t.length;i++)s+=t[i][0]*e[i],n+=t[i][1]*e[i];s/=h,n/=h;break;case r.COUNT_AVERAGE:h=0,i.forEach((t=>{h+=t}));for(let e=0;e<t.length;e++)s+=t[e][0]*i[e],n+=t[e][1]*i[e];s/=h,n/=h;break;default:for(let i=0;i<t.length;i++)s+=t[i][0],n+=t[i][1];s/=t.length,n/=t.length}return[s,n]}A(t){let i=0;switch(this.own.getOptions().clusterPointWeightType){case s.AVERAGE:t.weight_list.forEach((t=>{i+=t})),i/=t.weight_list.length;break;case s.MAX:t.weight_list.forEach((t=>{i<t&&(i=t)}));break;case s.SUM:t.weight_list.forEach((t=>{i+=t}));break;default:i=1}delete t.weight_list,t.weight=i}k(t){return t-this.points.length>>this.ZOOM_BITS}_(t){return(t-this.points.length)%32}L(t,i,e){if(t[i+this.OFFSET_NUM]>1){const s=this.clusterProps[t[i+this.OFFSET_PROP]];return e?Object.assign({},s):s}const s=this.points[t[i+this.OFFSET_ID]].properties;let r={};if(this.reduceType&G.INNER&&(r=this.innerMap(s)),this.own.getOptions().clusterReduce){let t=this.own.getOptions().clusterMap(s);Object.assign(r,t)}return e&&r===s?Object.assign({},r):r}l(t){var i,e;let s=this.own.getOptions();return Math.max(null!==(i=s.clusterMinZoom)&&void 0!==i?i:3,Math.min(Math.floor(+t),(null!==(e=s.clusterMaxZoom)&&void 0!==e?e:21)+1))}getClusters(t,i){var s;if(this.isIllegal(t))return[[],[]];let r=((i[0]+180)%360+360)%360-180;const n=Math.max(-90,Math.min(90,i[1]));let h=180===i[2]?180:((i[2]+180)%360+360)%360-180;const o=Math.max(-90,Math.min(90,i[3]));if(i[2]-i[0]>=360)r=-180,h=180;else if(r>h){const i=this.getClusters(t,[r,n,180,o]),e=this.getClusters(t,[-180,n,h,o]);return[[...i[0],...e[0]],[...i[1],...e[1]]]}let l=this.getClusterZoom(t);const a=this.getClusterTree(t);if(!a)return[[],[]];const{type:u,name:c}=this.getClusterType(t),f=a.range(k(r),_(o),k(h),_(n)),d=a.data,p=[],y=[];this.showLog;for(const t of f){const i=this.stride*t;if(this.showLog,d[i+this.OFFSET_NUM]>=this.own.getOptions().clusterMinPoints){let r=this.getClusterJSON(d,i,this.clusterProps);r.properties||(r.properties={}),r.properties.listChildren=this.getLeaves(d[i+this.OFFSET_ID],[],null!==(s=this.own.getOptions().clusterListChildren)&&void 0!==s?s:-1),r.properties.clusterIndex=(t<<this.ZOOM_BITS)+l,r.properties.allCount=this.points.length,r.properties.type=u,u!==e.GEO_FENCE&&u!==e.ATTR_REF||this.key_refs.has(d[i+this.OFFSET_ID])&&(r.properties.belongKey=c,r.properties.belongValue=this.key_refs.get(d[i+this.OFFSET_ID])),p.push(r)}else{let t=d[i+this.OFFSET_ID],e=this.points[t];e.id=t,e.properties?e.properties.listChildren=[]:e.properties={listChildren:[]},e.properties?e.properties.clusterId=t:e.properties={clusterId:t},e.properties?e.properties.allCount=this.points.length:e.properties={allCount:this.points.length},e.properties?e.properties.parentId=d[i+this.OFFSET_PARENT]:e.properties={parentId:d[i+this.OFFSET_PARENT]},y.push(e)}}return this.isSort&&p.sort(((t,i)=>{var e,s;return(null===(e=t.properties)||void 0===e?void 0:e.pointCount)-(null===(s=i.properties)||void 0===s?void 0:s.pointCount)})),[y,p]}getElementById(t,i,s){let r=s?i>>this.ZOOM_BITS:i,n=new T;n.id=t,n.index=r;let h=s?i&(1<<this.ZOOM_BITS)-1:this._(t)-1,{type:o,name:l}=this.getClusterType(h);if(n.type=o,t<=this.points.length)n.isCluster=!1,n.properties=this.points[t].properties,n.latLng=this.points[t].geometry.coordinates;else{n.isCluster=!0;let i=this.getClusterTree(h),s=i.data;if(i){let i=r*this.stride;this.showLog,n.latLng=[s[i],s[i+1]],n.pointCount=s[i+this.OFFSET_NUM],n.parentId=s[i+this.OFFSET_PARENT],o!==e.GEO_FENCE&&o!==e.ATTR_REF||this.key_refs.has(t)&&(n.belongKey=l,n.belongValue=this.key_refs.get(t)),this.reduceType!==G.NONE&&(n.reduces=this.clusterProps[s[i+this.OFFSET_PROP]]);const h=this.getLeaves(t,[],1/0);if(h&&h.length>0){const i=h.map((t=>t.latLng));i.length>0?(n.bbox=E(i,!0),this.showLog):this.cluster_geo_refs.has(t)&&(n.bbox=this.cluster_geo_refs.get(t).bbox)}else this.cluster_geo_refs.has(t)&&(n.bbox=this.cluster_geo_refs.get(t).bbox)}}return!n.bbox&&this.cluster_geo_refs.has(t)&&(n.bbox=this.cluster_geo_refs.get(t).bbox),this.showLog,n}getUnDistanceBrothers(t,i,e,s,r){let n=this.k(t),h=this.points[n],o=h?h["_inner_"+i+"_"+s]:void 0,l=[];return o&&(l=this.points.filter((t=>!!t&&t["_inner_"+i+"_"+s]===o))),l.slice(0,r).map((t=>{let e=new T;return e.id=t.id,e.index=t.id,e.type=i,e.isCluster=!1,e.properties=this.points[t.id].properties,e.latLng=this.points[t.id].geometry.coordinates,e}))}getLeaves(t,i=[],s){if(void 0===s&&(s=1/0),s<=0)return[];i||(i=[]);let r=this._(t)-1,{type:n,name:h}=this.getClusterType(r);if(n===e.GEO_FENCE||n===e.ATTR_REF)return this.getUnDistanceBrothers(t,n,h,r,s);if(t>this.points.length){let e=this.getClusterTree(r+1),n=e.data;if(e)for(let e=0;e<n.length/this.stride;e++){let r=e*this.stride+this.OFFSET_ID;if(n[e*this.stride+this.OFFSET_PARENT]===t)if(n[r]<=this.points.length){if(!(i.length<s))break;{let t=this.getElementById(n[r],e);i.push(t)}}else this.getLeaves(n[r],i,s)}}return i}getChildNodes(t){let i=[];if(t<=this.points.length)i=[];else{let e=this._(t)-1,s=this.getClusterTree(e+1),r=s.data;if(s)for(let e=0;e<r.length/this.stride;e++)if(r[e*this.stride+this.OFFSET_PARENT]===t){let s=this.getElementById(r[e*this.stride+this.OFFSET_ID],e);s.parentId=t,i.push(s)}}return i}getClusterJSON(t,i,e){return{type:"Feature",id:t[i+this.OFFSET_ID],properties:this.getClusterProperties(t,i,e),geometry:{type:"Point",coordinates:[t[i],t[i+1]]}}}getClusterProperties(t,i,e){const s=t[i+this.OFFSET_NUM],r=s>=1e4?`${Math.round(s/1e3)}k`:s>=1e3?Math.round(s/100)/10+"k":s;let n={};if(this.reduceType!==G.NONE){const s=t[i+this.OFFSET_PROP];n=-1===s?{}:Object.assign({},{reduces:e[s]})}return Object.assign(n,{isCluster:!0,clusterId:t[i+this.OFFSET_ID],parentId:t[i+this.OFFSET_PARENT],point:[t[i],t[i+1]],pointCount:s,pointCountAbbrev:r})}isIllegal(t){var i,e;let s=null!==(i=this.own.getOptions().minZoom)&&void 0!==i?i:3,r=null!==(e=this.own.getOptions().maxZoom)&&void 0!==e?e:23;return t<s||t>r}reset(){this.points=[],this.clusterProps=[],this.trees={},this.geo_refs.clear(),this.key_refs.clear(),this.cluster_geo_refs.clear()}}class P{constructor(t,i,e,s){this.T=600,this.G=[12,12,12,12],this.own=t,this.engine=i,this.map=e,this.showLog=s;let r=this.own.getOptions().fitViewMargin;r&&(this.G=r),this.register(),this.createLayers()}register(){this.own.getOptions().updateRealTime?(this.T=this.own.getOptions().waitTime||300,this.map.addEventListener("update",this.B=g(this.mapStatusChange.bind(this),this.T,{leading:!0,trailing:!1}))):this.map.addEventListener("mapstatusidle_inner",this.B=this.mapStatusChange.bind(this)),this.map.addEventListener("destroy",(()=>{this.own.destroy()})),this.own.on(i.DESTROY,(()=>{this.destroy()}))}unregister(){this.own.getOptions().updateRealTime?this.map.removeEventListener("update",this.B):this.map.removeEventListener("mapstatusidle_inner",this.B),this.B=()=>{}}mapStatusChange(t){let e=this.map.getZoom(),s=this.map.getBounds(),r=s.getSouthWest(),n=s.getNorthEast();this.showLog;let h=this.engine.getClusters(e,[r.lng,r.lat,n.lng,n.lat]);this.own.fire(i.CHANGE,h),this.own.isRender&&this.update(h)}update(t){if(this.showLog,this.own.getOptions().isAnimation){if(!(this.multi_layer instanceof BMapGL.CustomHtmlLayer))throw new Error("isAnimation is true, but renderClusterStyle is not dom type");this.render([...t[0],...t[1]],h.MULTI)}else this.render(t[0],h.SINGLE),this.render(t[1],h.MULTI)}render(t,i){this.showLog,this.showLog;let e={type:"FeatureCollection",features:t},s=i===h.MULTI?this.multi_layer:this.single_layer;s&&(t.length>0&&(null==s||s.setData(e)),0===t.length&&(null==s||s.clearData()))}createLayers(){var t,i;(null===(t=this.own.getOptions().renderSingleStyle)||void 0===t?void 0:t.type)===n.DOM?this.single_layer=this.createLayer(h.SINGLE,n.DOM):this.single_layer=this.createLayer(h.SINGLE,n.WEBGL),(null===(i=this.own.getOptions().renderClusterStyle)||void 0===i?void 0:i.type)===n.DOM?this.multi_layer=this.createLayer(h.MULTI,n.DOM):this.multi_layer=this.createLayer(h.MULTI,n.WEBGL)}createLayer(t,e){this.showLog;let s,r=this.own.getOptions(),o=t===h.MULTI?r.renderClusterStyle:r.renderSingleStyle;if(!o)return;if(e===n.DOM){let i={sliceRepeat:!0,minZoom:this.own.getOptions().minZoom,maxZoom:this.own.getOptions().maxZoom,zIndex:t===h.MULTI?10:9,nextTick:!0,fixBottom:!0,useTranslate:!!this.own.getOptions().isAnimation,anchors:[.5,.5],displayType:"normal",enableDraggingMap:!0};this.own.getOptions().isAnimation&&(i.displayType="cluster"),Object.assign(i,o.style);const e=new BMapGL.CustomHtmlLayer(o.inject,i);this.map.addCustomHtmlLayer(e),s=e}else{let i=1.2*(r.clusterRadius||32),e={iconObj:(t,i)=>({canvas:(null==o?void 0:o.inject)(i),id:i.clusterId}),sizes:[i,i/2],scale:1,userSizes:!1,anchors:[0,0],width:i,height:i/2};Object.assign(e,o.style);let n={icon:"https://webmap0.bdimg.com/image/api/marker_red.png",sizes:[8,8],anchors:[0,-1],userSizes:!1,width:["match",["get","type"],0,16,8],height:["match",["get","type"],0,16,8]};Object.assign(n,o.style);const l=new BMapGL.PointIconLayer({minZoom:this.own.getOptions().minZoom,maxZoom:this.own.getOptions().maxZoom,zIndex:t===h.MULTI?10:9,isTop:!1,enablePicked:!0,autoSelect:!1,pickWidth:30,pickHeight:30,opacity:1,isFlat:!1,isFixed:!0,style:t===h.MULTI?e:n});this.map.addNormalLayer(l),s=l}s.addEventListener("click",(t=>{if(this.showLog,t.target instanceof BMapGL.PointIconLayer&&!t.value.dataItem)return;let e=this.getElementByEvent(t);e.bbox&&Array.isArray(e.bbox)&&e.bbox[0]!==1/0&&this.fitView(e.bbox),this.own.fire(i.CLICK,e)}));let l=e===n.DOM?"mouseover":"mousemove";return s.addEventListener(l,(t=>{if(this.showLog,t.target instanceof BMapGL.PointIconLayer&&!t.value.dataItem)return void(this.P&&(this.own.fire(i.MOUSE_OUT,this.P),this.P=null));let e=this.getElementByEvent(t);if(t.target instanceof BMapGL.PointIconLayer)return this.P&&this.P.id===e.id||this.own.fire(i.MOUSE_OVER,e),void(this.P=e);this.own.fire(i.MOUSE_OVER,e)})),s.addEventListener("mouseout",(t=>{if(this.showLog,t.target instanceof BMapGL.PointIconLayer)return;let e=this.getElementByEvent(t);this.own.fire(i.MOUSE_OUT,e)})),s}getElementByEvent(t){var i,e;let s,r,n=[];if(t.target instanceof BMapGL.CustomOverlay){let e=t.target.properties||{};s=e.clusterId,r=e.clusterIndex,n=null!==(i=e.listChildren)&&void 0!==i?i:[]}else{let i=t.value.dataItem.properties||{};s=i.clusterId,r=i.clusterIndex,n=null!==(e=i.listChildren)&&void 0!==e?e:[]}let h=this.engine.getElementById(s,r,!0);return h.listChildren=n,h.pixel=[t.pixel.x,t.pixel.y],h.target=t.target,h}clearRender(){this.showLog,this.update([[],[]])}unrender(){this.showLog,this.multi_layer instanceof BMapGL.PointIconLayer?this.map.removeNormalLayer(this.multi_layer):this.map.removeCustomHtmlLayer(this.multi_layer),this.single_layer instanceof BMapGL.PointIconLayer?this.map.removeNormalLayer(this.single_layer):this.map.removeCustomHtmlLayer(this.single_layer),this.multi_layer=void 0,this.single_layer=void 0}fitView(t){if(this.showLog,this.own.getOptions().fitViewOnClick){let i=this.map.getZoom(),e=[new BMapGL.Point(t[0],t[1]),new BMapGL.Point(t[2],t[3])],s={margins:this.G,enableAnimation:!0};this.map.setViewport(e,s),setTimeout((()=>{i===this.map.getZoom()&&this.map.setZoom(i+1),this.mapStatusChange()}),300)}}show(){this.single_layer&&(this.single_layer instanceof BMapGL.CustomHtmlLayer&&this.single_layer.show(),this.single_layer instanceof BMapGL.PointIconLayer&&this.single_layer.setVisible(!0)),this.multi_layer&&(this.multi_layer instanceof BMapGL.CustomHtmlLayer&&this.multi_layer.show(),this.multi_layer instanceof BMapGL.PointIconLayer&&this.multi_layer.setVisible(!0))}hide(){this.single_layer&&(this.single_layer instanceof BMapGL.CustomHtmlLayer&&this.single_layer.hide(),this.single_layer instanceof BMapGL.PointIconLayer&&this.single_layer.setVisible(!1)),this.multi_layer&&(this.multi_layer instanceof BMapGL.CustomHtmlLayer&&this.multi_layer.hide(),this.multi_layer instanceof BMapGL.PointIconLayer&&this.multi_layer.setVisible(!1))}getSingleLayer(){return this.single_layer}getClusterLayer(){return this.multi_layer}drawMarker(t){this.map.addOverlay(new BMapGL.Marker(new BMapGL.Point(t[0],t[1])))}destroy(){this.map&&(this.unregister(),this.unrender())}}
return Object.freeze({__proto__:null,get ClusterData(){return h},ClusterElement:T,get ClusterEvent(){return i},get ClusterRender(){return n},get ClusterType(){return e},get LocCountType(){return r},get PointWeightType(){return s},View:class extends t{constructor(t,i){if(super(),this.Z=!1,!t)throw new Error("map is required");i&&this.verifyOptions(i);const e={tileSize:256,minZoom:3,maxZoom:21,clusterRadius:30,clusterMinZoom:3,clusterMaxZoom:16,clusterMinPoints:2,clusterListChildren:0,clusterPointWeight:void 0,clusterPointWeightType:s.SUM,clusterPointLocationType:r.WEIGHT_AVERAGE,clusterPointType:void 0,clusterMap:t=>({}),clusterReduce:void 0,clusterType:void 0,clusterDictionary:void 0,isRender:!0,isAnimation:!1,renderClusterStyle:{type:n.DOM,style:{},inject:t=>{var i=Math.pow(t.pointCount/t.allCount,.1),e=document.createElement("div"),s=180-180*i,r="hsla("+s+",100%,30%,0.7)",n="hsla("+s+",100%,90%,1)",h="hsla("+s+",100%,30%,1)",o="hsla("+s+",100%,90%,1)";e.style.backgroundColor=r;var l=Math.round(25+30*Math.pow(t.pointCount/t.allCount,1/6));return e.style.width=e.style.height=l+"px",e.style.border="solid 1px "+h,e.style.borderRadius=l/2+"px",e.style.boxShadow="0 0 5px "+o,e.innerHTML=t.pointCount,e.style.lineHeight=l+"px",e.style.color=n,e.style.fontSize="14px",e.style.textAlign="center",e.style.cursor="pointer",e}},renderSingleStyle:{type:n.WEBGL,style:{},inject:t=>document.createElement("canvas")},fitViewOnClick:!0,fitViewMargin:void 0,updateRealTime:!1,waitTime:300};this.options=Object.assign(e,i),this.engine=new B(this,this.Z),this.render=new P(this,this.engine,t,this.Z),this.isRender=this.options.isRender||!1}setData(t){this.Z,this.engine.createClusters(t),this.redraw()}getSingleLayer(){return this.render.getSingleLayer()}getClusterLayer(){return this.render.getClusterLayer()}getLeaves(t,i){return this.engine.getLeaves(t,[],i)}getSonNodes(t){return this.engine.getChildNodes(t)}redraw(){this.render.mapStatusChange()}show(){this.render.show()}hide(){this.render.hide()}destroy(){this.fire(i.DESTROY)}get isRender(){return this.O}set isRender(t){this.O=t,!t&&this.render.clearRender()}getOptions(){return this.options}verifyOptions(t){var i,e;if(t.minZoom&&t.minZoom<3)throw new Error("minZoom must be greater than 3");if(t.maxZoom&&t.maxZoom>23)throw new Error("maxZoom must be less than 23");if(Math.max(null!==(i=t.minZoom)&&void 0!==i?i:3,3)>Math.min(null!==(e=t.maxZoom)&&void 0!==e?e:23,23))throw new Error("minZoom must be less than maxZoom");if(t.clusterMinZoom&&t.clusterMinZoom<3)throw new Error("clusterMinZoom must be greater than 3");if(t.clusterMaxZoom&&t.clusterMaxZoom>23)throw new Error("clusterMaxZoom must be less than 23");if(t.clusterMinZoom&&t.clusterMaxZoom&&t.clusterMinZoom>t.clusterMaxZoom)throw new Error("clusterMinZoom must be less than clusterMaxZoom");if(t.clusterType){if(!(t.clusterType instanceof Array))throw new Error("clusterType must be an array");if(t.clusterType.length<1)throw new Error("clusterType must be greater than 0");for(let i=0;i<t.clusterType.length;i++){if(t.clusterType[i].length<4)throw new Error("clusterType must be greater than 4");if(!t.clusterType[i][0])throw new Error("clusterType item startZoom must not be null");if(t.clusterType[i][1]&&t.clusterType[i][0]>t.clusterType[i][1])throw new Error("clusterType item endZoom must be less than starZoom");if(null===t.clusterType[i][1]&&i<t.clusterType.length-1)throw new Error("clusterType item endZoom must not be null,excluding the last item endZoom");if(i<t.clusterType.length-1&&null!==t.clusterType[i][1]&&t.clusterType[i][1]>t.clusterType[i+1][0])throw new Error("clusterType item endZoom must be less than the next startZoom")}}}},pointTransformer:(t,i)=>{let e=[];return t.forEach(((t,s)=>{const{point:r=null,properties:n={}}=i(t,s);r&&e.push({type:"Feature",geometry:{type:"Point",coordinates:r},properties:n})})),e}})}));
