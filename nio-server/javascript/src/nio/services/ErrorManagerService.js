const translateObject = {
    organisation: {
        key: {
            required: "La clé de l'organisation est obligatoire.",
            invalid: "Le format de la clé de l'organisation est incorrect (un mot sans espace ni caractère spécial)."
        },
        label: {
            required: "Le libellé de l'organisation est obligatoire."
        },
        diff: {
            numV1: {
                required: "La version est obligatoire."
            },
            numV2: {
                required: "La version est obligatoire."
            }
        },
        groups: {
            x: {
                key: {
                    required: "La clé du groupe est obligatoire.",
                    invalid: "Le format de la clé du groupe est incorrect (un mot sans espace ni caractère spécial)."
                },
                label: {
                    required: "Le libellé du groupe est obligatoire."
                },
                permissions: {
                    x: {
                        key: {
                            required: "La clé de la permission est obligatoire.",
                            invalid: "Le format de la clé de la permission est incorrect (un mot sans espace ni caractère spécial)."
                        },
                        label: {
                            required: "Le libellé de la permission est obligatoire."
                        }
                    }
                }
            }
        },
        offers: {
            x: {
                key: {
                    required: "La clé de l'offre est obligatoire.",
                    invalid: "Le format de la clé de l'offre est incorrect (un mot sans espace ni caractère spécial)."
                },
                label: {
                    required: "Le libellé de l'offre est obligatoire."
                },
                groups: {
                    x: {
                        key: {
                            required: "La clé du groupe de l'offre est obligatoire.",
                            invalid: "Le format de la clé du groupe de l'offre est incorrect (un mot sans espace ni caractère spécial)."
                        },
                        label: {
                            required: "Le libellé du groupe de l'offre est obligatoire."
                        },
                        permissions: {
                            x: {
                                key: {
                                    required: "La clé de la permission de l'offre est obligatoire.",
                                    invalid: "Le format de la clé de la permission de l'offre est incorrect (un mot sans espace ni caractère spécial)."
                                },
                                label: {
                                    required: "Le libellé de la permission de l'offre est obligatoire."
                                }
                            }
                        }
                    }
                }
            }
        }
    },
    consentsSample: {
        organisationKey: {
            required: "La clé de l'organisation est obligatoire.",
            not: {
                found: "La clé de l'organisation n'est pas reconnue"
            }
        },
        userId: {
            required: "L'identifiant de l'utilisateur est obligatoire."
        }
    },
    offers: {
        x: {
            key: {
                required: "La clé de l'offre est obligatoire.",
                invalid: "Le format de la clé de l'offre est incorrect (un mot sans espace ni caractère spécial)."
            },
            label: {
                required: "Le libellé de l'offre est obligatoire."
            },
            groups: {
                required: "Au moins un groupe doit être défini.",
                x: {
                    key: {
                        required: "La clé du groupe est obligatoire.",
                        invalid: "Le format de la clé du groupe est incorrect (un mot sans espace ni caractère spécial)."
                    },
                    label: {
                        required: "Le libellé du groupe est obligatoire."
                    },
                    permissions: {
                        required: "Au moins une permission doit être définie.",
                        x: {
                            key: {
                                required: "La clé de la permission est obligatoire.",
                                invalid: "Le format de la clé de la permission est incorrect (un mot sans espace ni caractère spécial)."
                            },
                            label: {
                                required: "Le libellé de la permission est obligatoire."
                            }
                        }
                    }
                }
            }
        }
    },
    account: {
        email: {
            required: "L'email est obligatoire.",
            invalidFormat: "Le format de l'email est incorrecte."
        },
        password: {
            required: "Le mot de passe est obligatoire."
        },
        confirmPassword: {
            required: "La confirmation de mot de passe est obligatoire.",
            invalid: "Le mot de passe est différent de la valeur saisie."
        },
        clientId: {
            required: "Le client id est obligatoire.",
            invalidFormat: "Le format du client id est invalide (il doit des lettres et des chiffres uniquement)"
        },
        clientSecret: {
            required: "Le client secret est obligatoire.",
            invalidFormat: "Le format du client secret est invalide (il doit des lettres et des chiffres uniquement)"
        }
    }
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