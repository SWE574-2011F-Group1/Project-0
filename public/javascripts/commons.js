function isNumber(n) {
    return !isNaN(parseFloat(n)) && isFinite(n);
}

function leftPadWithZeros(s, l) {
    while (s.length < l) {
        s = '0' + s;
    }
    return s;
}
