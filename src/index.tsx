import { NativeModules } from 'react-native';

type NielsenNzmeType = {
  multiply(a: number, b: number): Promise<number>;
};

const { NielsenNzme } = NativeModules;

export default NielsenNzme as NielsenNzmeType;
