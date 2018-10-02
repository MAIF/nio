const translateObject = {

};

export function translate(key, args = []) {
    let searchKey = key.split(".").map(k => {
        if (/^\d+$/.test(k)) {
            return "x";
        }
        return k;
    }).join(".");

    const patternMessage = findProp(translateObject, searchKey) || '';
    return args.reduce((acc, arg, i) => acc.replace(`{${i}}`, arg), patternMessage);
}

function findProp(obj, prop, defval) {
    if (typeof defval === 'undefined') defval = null;
    prop = prop.split('.');
    for (let i = 0; i < prop.length; i++) {
        const tmpProp = prop[i];
        if (typeof obj[tmpProp] === 'undefined')
            return defval;
        obj = obj[tmpProp];
    }
    return obj;
}