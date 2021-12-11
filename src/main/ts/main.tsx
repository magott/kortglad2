import React, { useCallback, useState } from "react";
import ReactDOM from "react-dom";

interface Props { message: string }

const Main:React.VFC<Props> = ({message}) => {
    const [count, setCount] = useState(0);
    const increment = useCallback(() => {
        setCount(count => count + 1);
    }, [count]);
    return(<>
        <h1>{message}</h1>
        <h2>Count: {count}</h2>
        <button onClick={increment}>Increment</button>
    </>)
}

ReactDOM.render(
  <Main message="Hello World! Simple Counter App built on ESBuild + React + Typescript"/>,
  document.getElementById('root')  
);