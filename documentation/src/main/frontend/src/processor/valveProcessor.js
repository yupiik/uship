export const processValves = ({ valves, ...rest }) => ({
    ...rest,
    valves: valves
        .map(valve => valve.name === 'Extended Access Log Valve' && valve.attributes.length <= 5 ?
            {
                ...valve,
                attributes: [
                    ...valve.attributes,
                    ...valves
                        .filter(v => v.name === 'Access Log Valve')
                        .reduce((aggregator, inheritedValve) => ([
                            ...aggregator,
                            ...(inheritedValve.attributes || [])
                                .filter(newAttribute => !valve.attributes.some(n => n.name === newAttribute.name)),
                        ]), []),
                ],
            } :
            valve),
});
